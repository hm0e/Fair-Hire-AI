package com.fairhire.models.enums;

import java.math.BigDecimal;

/**
 * Transparent, deterministic extraction methods with explicit heuristic confidence weights.
 * These confidence values represent rule-based match tiers rather than calibrated statistical probabilities.
 */
public enum SkillExtractionMethod {

    /**
     * Exact case and boundary match against canonical skill name (e.g., "Java" -> "Java").
     */
    EXACT_CANONICAL_MATCH(new BigDecimal("1.000")),

    /**
     * Case-insensitive match against canonical skill name (e.g., "java" -> "Java").
     */
    CASE_INSENSITIVE_CANONICAL_MATCH(new BigDecimal("0.970")),

    /**
     * Exact case and boundary match against a curated synonym/alias (e.g., "SpringBoot" -> "Spring Boot").
     */
    EXACT_ALIAS_MATCH(new BigDecimal("0.950")),

    /**
     * Case-insensitive match against a curated synonym/alias (e.g., "springboot" -> "Spring Boot").
     */
    CASE_INSENSITIVE_ALIAS_MATCH(new BigDecimal("0.920")),

    /**
     * Match resolved via token whitespace, hyphen, or punctuation normalization (e.g., "Spring-Boot" -> "Spring Boot").
     */
    NORMALIZED_MATCH(new BigDecimal("0.880"));

    private final BigDecimal defaultConfidence;

    SkillExtractionMethod(BigDecimal defaultConfidence) {
        this.defaultConfidence = defaultConfidence;
    }

    public BigDecimal getDefaultConfidence() {
        return defaultConfidence;
    }
}
