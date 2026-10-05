package com.fairhire.services;

import com.fairhire.dto.ExtractedSkillDto;
import com.fairhire.dto.ResumeUploadResponse;
import com.fairhire.models.Candidate;
import com.fairhire.models.Resume;
import com.fairhire.models.enums.ParsingStatus;
import com.fairhire.models.enums.ResumeFileType;
import com.fairhire.repositories.CandidateRepository;
import com.fairhire.repositories.ResumeRepository;
import com.fairhire.services.parser.*;
import com.fairhire.services.skill.SkillExtractionService;
import com.fairhire.storage.StorageService;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service orchestrating deterministic resume ingestion, cryptographic hashing,
 * secure storage, format-specific text extraction, text normalization, and
 * canonical skill extraction.
 */
@Service
public class ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);

    private final ResumeRepository resumeRepository;
    private final CandidateRepository candidateRepository;
    private final StorageService storageService;
    private final ResumeValidator resumeValidator;
    private final TextNormalizer textNormalizer;
    private final List<ResumeTextExtractor> textExtractors;
    private final SkillExtractionService skillExtractionService;
    private final String parserVersion;

    public ResumeService(
            ResumeRepository resumeRepository,
            CandidateRepository candidateRepository,
            StorageService storageService,
            ResumeValidator resumeValidator,
            TextNormalizer textNormalizer,
            List<ResumeTextExtractor> textExtractors,
            SkillExtractionService skillExtractionService,
            @Value("${fairhire.parser.version:v1.0-deterministic}") String parserVersion
    ) {
        this.resumeRepository = resumeRepository;
        this.candidateRepository = candidateRepository;
        this.storageService = storageService;
        this.resumeValidator = resumeValidator;
        this.textNormalizer = textNormalizer;
        this.textExtractors = textExtractors;
        this.skillExtractionService = skillExtractionService;
        this.parserVersion = parserVersion;
    }

    /**
     * Ingest, validate, hash, store, deterministically extract text, and extract canonical skills
     * from an uploaded resume file.
     *
     * @param file           Multipart file upload
     * @param candidateId    Optional existing candidate ID
     * @param candidateName  Optional candidate name for automatic profile creation
     * @param candidateEmail Optional candidate email for automatic profile creation
     * @return Formatted {@link ResumeUploadResponse}
     * @throws InvalidResumeUploadException On validation failure (MIME, magic byte, size, extension)
     * @throws DuplicateResumeException     If an identical resume file already exists
     * @throws IOException                  On file stream read failure
     */
    @Transactional
    public ResumeUploadResponse ingestResume(
            MultipartFile file,
            Long candidateId,
            String candidateName,
            String candidateEmail
    ) throws IOException {

        // 1. Rigorous upload validation (extension, size, MIME type, magic byte verification, path traversal checks)
        ResumeFileType fileType = resumeValidator.validate(file);
        byte[] fileBytes = file.getBytes();

        // 2. Cryptographic SHA-256 hash generation of original binary content
        String sha256 = computeSha256(fileBytes);

        // 3. Duplicate detection
        Optional<Resume> existing = resumeRepository.findByFileHashSha256(sha256);
        if (existing.isPresent()) {
            throw new DuplicateResumeException(
                    "Upload rejected: A resume with identical SHA-256 hash already exists.",
                    sha256,
                    existing.get().getId()
            );
        }

        // 4. Resolve or initialize Candidate entity
        Candidate candidate = resolveCandidate(candidateId, candidateName, candidateEmail);

        // 5. Store binary content via storage abstraction (never stores absolute local path)
        String sanitizedFilename = FilenameUtils.getName(file.getOriginalFilename());
        String storageKey = storageService.store(fileBytes, sanitizedFilename, sha256);

        // 6. Persist initial Resume entity in UPLOADED state
        Resume resume = new Resume();
        resume.setCandidate(candidate);
        resume.setFileName(sanitizedFilename);
        resume.setFileType(fileType);
        resume.setFileHashSha256(sha256);
        resume.setFileSizeBytes((long) fileBytes.length);
        resume.setFilePath(storageKey);
        resume.setParserVersion(parserVersion);
        resume.setParsingStatus(ParsingStatus.UPLOADED);
        resume.setRawText("");
        resume = resumeRepository.save(resume);

        // 7. Transition to PARSING state
        resume.setParsingStatus(ParsingStatus.PARSING);
        resume = resumeRepository.save(resume);

        // 8. Deterministic Text Extraction
        ResumeTextExtractor extractor = textExtractors.stream()
                .filter(e -> e.supports(fileType))
                .findFirst()
                .orElseThrow(() -> new TextExtractionException("No extractor available for file type: " + fileType));

            try {
            ExtractionResult extractionResult = extractor.extract(fileBytes);

            // 9. Deterministic Text Normalization
            String normalizedText = textNormalizer.normalize(extractionResult.rawText());

            if (normalizedText == null || normalizedText.isBlank()) {
                log.warn("Resume ID {} contains no extractable text content after normalization.", resume.getId());
                resume.setRawText("");
                resume.setParsingStatus(ParsingStatus.FAILED);
                resume.setParsingError("Text extraction produced empty content: Document contains no readable text.");
            } else {
                resume.setRawText(normalizedText);
                resume.setParsingStatus(ParsingStatus.PARSED);
                resume.setParsingError(null);
                log.info("Successfully extracted and normalized text for resume ID {} using {}", resume.getId(), extractionResult.parserEngine());
            }

        } catch (OcrRequiredException e) {
            log.warn("Resume ID {} contains image-only content requiring OCR: {}", resume.getId(), e.getMessage());
            resume.setParsingStatus(ParsingStatus.FAILED);
            resume.setParsingError(e.getMessage());
            resume.setRawText("");
        } catch (TextExtractionException e) {
            log.error("Text extraction failed for resume ID {}: {}", resume.getId(), e.getMessage());
            resume.setParsingStatus(ParsingStatus.FAILED);
            resume.setParsingError(e.getMessage());
            resume.setRawText("");
        }

        resume = resumeRepository.save(resume);

        // 10. Deterministic Skill Extraction (Phase 3B)
        List<ExtractedSkillDto> extractedSkills = Collections.emptyList();
        if (resume.getParsingStatus() == ParsingStatus.PARSED) {
            extractedSkills = skillExtractionService.extractAndSaveSkills(resume);
        }

        return ResumeUploadResponse.fromEntity(resume, extractedSkills);
    }

    /**
     * Backwards-compatibility alias for raw text resume seeding/ingestion.
     */
    @Transactional
    public ResumeUploadResponse parseAndSaveResume(String rawText, String filePath, String name, String email) {
        return ingestRawText(rawText, name, email);
    }

    /**
     * Fallback for direct plain-text resume ingestion (useful for testing and benchmark pairs).
     */
    @Transactional
    public ResumeUploadResponse ingestRawText(String rawText, String name, String email) {
        if (rawText == null || rawText.replace("\uFEFF", "").isBlank()) {
            throw new InvalidResumeUploadException("Upload rejected: Raw text cannot be empty or whitespace-only.");
        }

        String normalizedText = textNormalizer.normalize(rawText);
        if (normalizedText.isBlank()) {
            throw new InvalidResumeUploadException("Upload rejected: Raw text contains only whitespace.");
        }

        byte[] textBytes = rawText.getBytes(StandardCharsets.UTF_8);
        String sha256 = computeSha256(textBytes);

        Optional<Resume> existing = resumeRepository.findByFileHashSha256(sha256);
        if (existing.isPresent()) {
            throw new DuplicateResumeException(
                    "Upload rejected: A resume with identical SHA-256 hash already exists.",
                    sha256,
                    existing.get().getId()
            );
        }

        Candidate candidate = resolveCandidate(null, name, email);

        Resume resume = new Resume();
        resume.setCandidate(candidate);
        resume.setFileName("raw_resume_" + System.currentTimeMillis() + ".txt");
        resume.setFileType(ResumeFileType.TXT);
        resume.setFileHashSha256(sha256);
        resume.setFileSizeBytes((long) textBytes.length);
        resume.setRawText(normalizedText);
        resume.setParserVersion(parserVersion);
        resume.setParsingStatus(ParsingStatus.PARSED);
        resume = resumeRepository.save(resume);

        // Deterministic Skill Extraction (Phase 3B)
        List<ExtractedSkillDto> extractedSkills = skillExtractionService.extractAndSaveSkills(resume);

        return ResumeUploadResponse.fromEntity(resume, extractedSkills);
    }

    @Transactional(readOnly = true)
    public List<ResumeUploadResponse> getAllResumes() {
        return resumeRepository.findAllByOrderByIdDesc().stream()
                .map(ResumeUploadResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumeUploadResponse getResumeById(Long id) {
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resume with ID " + id + " not found."));
        return ResumeUploadResponse.fromEntity(resume);
    }

    @Transactional
    public void deleteResume(Long id) {
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resume with ID " + id + " does not exist."));

        if (resume.getFilePath() != null) {
            try {
                storageService.delete(resume.getFilePath());
            } catch (IOException e) {
                log.warn("Failed to delete underlying document storage for resume ID {}: {}", id, e.getMessage());
            }
        }
        resumeRepository.delete(resume);
    }

    private Candidate resolveCandidate(Long candidateId, String candidateName, String candidateEmail) {
        if (candidateId != null) {
            return candidateRepository.findById(candidateId)
                    .orElseThrow(() -> new IllegalArgumentException("Candidate with ID " + candidateId + " not found."));
        }

        String candEmail = (candidateEmail != null && !candidateEmail.isBlank())
                ? candidateEmail.trim().toLowerCase()
                : ("candidate." + System.currentTimeMillis() + "@fairhire.local");

        return candidateRepository.findByEmail(candEmail).orElseGet(() -> {
            String candName = (candidateName != null && !candidateName.isBlank())
                    ? candidateName.trim()
                    : "Candidate " + System.currentTimeMillis();
            return candidateRepository.save(new Candidate(candName, candEmail));
        });
    }

    public static String computeSha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable in JVM", e);
        }
    }
}
