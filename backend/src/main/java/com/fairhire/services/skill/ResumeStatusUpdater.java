package com.fairhire.services.skill;

import com.fairhire.models.Resume;
import com.fairhire.models.enums.ParsingStatus;
import com.fairhire.repositories.ResumeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Isolated transaction helper for updating resume lifecycle status.
 * Ensures error states (such as SKILL_EXTRACTION_FAILED) are safely committed
 * in an independent transaction (REQUIRES_NEW) even when the calling transaction rolls back.
 */
@Component
public class ResumeStatusUpdater {

    private static final Logger log = LoggerFactory.getLogger(ResumeStatusUpdater.class);
    private final ResumeRepository resumeRepository;

    public ResumeStatusUpdater(ResumeRepository resumeRepository) {
        this.resumeRepository = resumeRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSkillExtractionFailed(Long resumeId, String errorMessage) {
        if (resumeId == null) return;
        try {
            resumeRepository.updateParsingStatusAndError(
                    resumeId,
                    ParsingStatus.SKILL_EXTRACTION_FAILED,
                    errorMessage,
                    Instant.now()
            );
            log.info("Persisted SKILL_EXTRACTION_FAILED status for resume ID {} in isolated transaction", resumeId);
        } catch (Exception ex) {
            log.error("Failed to persist SKILL_EXTRACTION_FAILED status for resume ID {}: {}", resumeId, ex.getMessage(), ex);
        }
    }
}
