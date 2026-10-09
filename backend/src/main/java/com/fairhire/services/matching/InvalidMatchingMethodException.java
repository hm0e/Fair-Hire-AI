package com.fairhire.services.matching;

/**
 * Exception thrown when an unapproved matching method is requested for new match execution.
 * Only CANONICAL_SKILL_COVERAGE is permitted for Phase 4 deterministic matching.
 */
public class InvalidMatchingMethodException extends RuntimeException {
    public InvalidMatchingMethodException(String message) {
        super(message);
    }
}
