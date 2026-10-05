package com.fairhire.dto;

import com.fairhire.models.Resume;
import com.fairhire.models.enums.ParsingStatus;
import com.fairhire.models.enums.ResumeFileType;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Public API response for uploaded and parsed resumes.
 * Exposes file metadata, hash integrity, parsing status, extracted text metrics,
 * and canonical extracted skills while strictly hiding internal filesystem storage locations.
 */
public record ResumeUploadResponse(
        Long resumeId,
        Long candidateId,
        String candidateName,
        String candidateEmail,
        String fileName,
        ResumeFileType fileType,
        Long fileSizeBytes,
        String fileHashSha256,
        ParsingStatus parsingStatus,
        String parserVersion,
        String rawText,
        Integer charCount,
        Integer wordCount,
        List<ExtractedSkillDto> extractedSkills,
        Integer skillsCount,
        String parsingError,
        Instant createdAt
) {
    public static ResumeUploadResponse fromEntity(Resume resume) {
        return fromEntity(resume, null);
    }

    public static ResumeUploadResponse fromEntity(Resume resume, List<ExtractedSkillDto> skillsOverride) {
        String text = resume.getRawText();
        int chars = (text != null) ? text.length() : 0;
        int words = (text != null && !text.isBlank()) ? text.trim().split("\\s+").length : 0;

        List<ExtractedSkillDto> skillDtos;
        if (skillsOverride != null) {
            skillDtos = skillsOverride;
        } else if (resume.getResumeSkills() != null && !resume.getResumeSkills().isEmpty()) {
            skillDtos = resume.getResumeSkills().stream()
                    .map(ExtractedSkillDto::fromEntity)
                    .toList();
        } else {
            skillDtos = Collections.emptyList();
        }

        return new ResumeUploadResponse(
                resume.getId(),
                resume.getCandidate() != null ? resume.getCandidate().getId() : null,
                resume.getCandidate() != null ? resume.getCandidate().getFullName() : null,
                resume.getCandidate() != null ? resume.getCandidate().getEmail() : null,
                resume.getFileName(),
                resume.getFileType(),
                resume.getFileSizeBytes(),
                resume.getFileHashSha256(),
                resume.getParsingStatus(),
                resume.getParserVersion(),
                resume.getRawText(),
                chars,
                words,
                skillDtos,
                skillDtos.size(),
                resume.getParsingError(),
                resume.getCreatedAt()
        );
    }
}
