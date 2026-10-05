package com.fairhire.services.skill;

import com.fairhire.models.Skill;
import com.fairhire.models.enums.SkillExtractionMethod;

import java.math.BigDecimal;

/**
 * Immutable candidate representation of an extracted skill mention prior to deduplication and persistence.
 */
public record ExtractedSkillCandidate(
        Skill skill,
        String canonicalName,
        String matchedText,
        String contextSnippet,
        BigDecimal confidence,
        SkillExtractionMethod method,
        int startPosition,
        int endPosition
) implements Comparable<ExtractedSkillCandidate> {

    @Override
    public int compareTo(ExtractedSkillCandidate other) {
        // Sort primarily by highest confidence descending, then by position ascending
        int confComparison = other.confidence.compareTo(this.confidence);
        if (confComparison != 0) {
            return confComparison;
        }
        return Integer.compare(this.startPosition, other.startPosition);
    }
}
