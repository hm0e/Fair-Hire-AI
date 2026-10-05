package com.fairhire.dto;

import com.fairhire.models.ResumeSkill;
import com.fairhire.services.skill.ExtractedSkillCandidate;

import java.math.BigDecimal;

/**
 * Data transfer object representing an extracted candidate skill with audit and context metadata.
 */
public record ExtractedSkillDto(
        Long skillId,
        String skillName,
        String category,
        String matchedText,
        String contextSnippet,
        BigDecimal extractionConfidence,
        String extractionMethod
) {
    public static ExtractedSkillDto fromCandidate(ExtractedSkillCandidate candidate) {
        return new ExtractedSkillDto(
                candidate.skill().getId(),
                candidate.canonicalName(),
                candidate.skill().getCategory(),
                candidate.matchedText(),
                candidate.contextSnippet(),
                candidate.confidence(),
                candidate.method().name()
        );
    }

    public static ExtractedSkillDto fromEntity(ResumeSkill entity) {
        return new ExtractedSkillDto(
                entity.getSkill().getId(),
                entity.getSkill().getName(),
                entity.getSkill().getCategory(),
                entity.getMatchedText() != null ? entity.getMatchedText() : entity.getSkill().getName(),
                entity.getContextSnippet(),
                entity.getExtractionConfidence(),
                entity.getExtractionMethod()
        );
    }
}
