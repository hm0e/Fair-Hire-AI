package com.fairhire.services.skill;

import com.fairhire.dto.ExtractedSkillDto;
import com.fairhire.models.Resume;
import com.fairhire.models.Skill;
import com.fairhire.models.enums.ParsingStatus;
import com.fairhire.repositories.ResumeRepository;
import com.fairhire.repositories.ResumeSkillRepository;
import com.fairhire.repositories.SkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service orchestrating database-driven skill extraction, persistence, and resume lifecycle management.
 * Provides concurrency controls to serialize concurrent re-extractions per resume and guarantees
 * atomic skill persistence and failure status persistence in isolated transactions.
 */
@Service
public class SkillExtractionService {

    private static final Logger log = LoggerFactory.getLogger(SkillExtractionService.class);

    private final SkillRepository skillRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeSkillRepository resumeSkillRepository;
    private final DeterministicSkillExtractor skillExtractor;
    private final SkillPersistenceService skillPersistenceService;
    private final ResumeStatusUpdater resumeStatusUpdater;

    private final ConcurrentHashMap<Long, Object> resumeLocks = new ConcurrentHashMap<>();

    public SkillExtractionService(
            SkillRepository skillRepository,
            ResumeRepository resumeRepository,
            ResumeSkillRepository resumeSkillRepository,
            DeterministicSkillExtractor skillExtractor,
            SkillPersistenceService skillPersistenceService,
            ResumeStatusUpdater resumeStatusUpdater
    ) {
        this.skillRepository = skillRepository;
        this.resumeRepository = resumeRepository;
        this.resumeSkillRepository = resumeSkillRepository;
        this.skillExtractor = skillExtractor;
        this.skillPersistenceService = skillPersistenceService;
        this.resumeStatusUpdater = resumeStatusUpdater;
    }

    private Object getLock(Long resumeId) {
        return resumeLocks.computeIfAbsent(resumeId, id -> new Object());
    }

    /**
     * Extract canonical skills from a parsed resume and persist them to the database.
     * Manages lifecycle transition from PARSED -> SKILL_EXTRACTION -> READY.
     * Serializes concurrent executions for the same resume entity.
     *
     * @param resume Persisted resume entity with extracted raw text
     * @return List of extracted skill DTOs
     */
    public List<ExtractedSkillDto> extractAndSaveSkills(Resume resume) {
        if (resume == null) {
            throw new IllegalArgumentException("Resume cannot be null for skill extraction.");
        }

        if (resume.getId() != null) {
            synchronized (getLock(resume.getId())) {
                return doExtractAndSaveSkills(resume);
            }
        }
        return doExtractAndSaveSkills(resume);
    }

    private List<ExtractedSkillDto> doExtractAndSaveSkills(Resume resume) {
        // B-06: Blank / whitespace-only resume text must never transition to READY
        if (resume.getRawText() == null || resume.getRawText().isBlank()) {
            log.warn("Resume ID {} has empty raw text, failing skill extraction.", resume.getId());
            if (resume.getId() != null) {
                resumeRepository.updateParsingStatusAndError(resume.getId(), ParsingStatus.FAILED, "Cannot extract skills: Resume text is empty or blank.", Instant.now());
            }
            resume.setParsingStatus(ParsingStatus.FAILED);
            resume.setParsingError("Cannot extract skills: Resume text is empty or blank.");
            return Collections.emptyList();
        }

        try {
            // 1. Transition state to SKILL_EXTRACTION
            if (resume.getId() != null) {
                resumeRepository.updateParsingStatusAndError(resume.getId(), ParsingStatus.SKILL_EXTRACTION, null, Instant.now());
            }
            resume.setParsingStatus(ParsingStatus.SKILL_EXTRACTION);

            // 2. Fetch active taxonomy skills
            List<Skill> activeSkills = skillRepository.findByIsActiveTrue();
            if (activeSkills.isEmpty()) {
                log.warn("No active skills found in taxonomy catalog.");
                if (resume.getId() != null) {
                    resumeRepository.updateParsingStatusAndError(resume.getId(), ParsingStatus.READY, null, Instant.now());
                }
                resume.setParsingStatus(ParsingStatus.READY);
                return Collections.emptyList();
            }

            // 3. Perform deterministic extraction
            List<ExtractedSkillCandidate> candidates = skillExtractor.extractSkills(resume.getRawText(), activeSkills);

            // 4 & 5. Atomic persistence of extracted skills in isolated transaction (B-07 / B-08)
            skillPersistenceService.replaceSkills(resume, candidates);

            // 6. Transition lifecycle state to READY
            if (resume.getId() != null) {
                resumeRepository.updateParsingStatusAndError(resume.getId(), ParsingStatus.READY, null, Instant.now());
            }
            resume.setParsingStatus(ParsingStatus.READY);
            resume.setParsingError(null);

            log.info("Successfully extracted and persisted {} canonical skills for resume ID {}",
                    candidates.size(), resume.getId());

            return candidates.stream()
                    .map(ExtractedSkillDto::fromCandidate)
                    .toList();

        } catch (Exception e) {
            log.error("Skill extraction failed for resume ID {}: {}", resume.getId(), e.getMessage(), e);
            // B-08: Safely persist failure state in dedicated independent transaction
            resumeStatusUpdater.markSkillExtractionFailed(resume.getId(), "Skill extraction failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Trigger skill extraction on an existing resume by ID with concurrency serialization.
     */
    public List<ExtractedSkillDto> extractSkillsForResumeId(Long resumeId) {
        if (!resumeRepository.existsById(resumeId)) {
            throw new IllegalArgumentException("Resume with ID " + resumeId + " not found.");
        }

        synchronized (getLock(resumeId)) {
            Resume resume = resumeRepository.findById(resumeId)
                    .orElseThrow(() -> new IllegalArgumentException("Resume with ID " + resumeId + " not found."));
            return doExtractAndSaveSkills(resume);
        }
    }

    /**
     * Fetch existing extracted skills for a resume.
     * B-05: Validates resume existence before returning skills list (throws IllegalArgumentException if non-existent).
     */
    @Transactional(readOnly = true)
    public List<ExtractedSkillDto> getSkillsForResume(Long resumeId) {
        if (!resumeRepository.existsById(resumeId)) {
            throw new IllegalArgumentException("Resume with ID " + resumeId + " not found.");
        }
        return resumeSkillRepository.findByResumeId(resumeId).stream()
                .map(ExtractedSkillDto::fromEntity)
                .toList();
    }
}
