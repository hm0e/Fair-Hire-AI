package com.fairhire.services.skill;

import com.fairhire.models.Resume;
import com.fairhire.models.ResumeSkill;
import com.fairhire.repositories.MatchResultRepository;
import com.fairhire.repositories.ResumeSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Isolated transaction service for replacing skills of a resume.
 * Executes skill deletion and persistence in an isolated transaction
 * so any failure rolls back partial skills cleanly without affecting
 * parent lifecycle status recording.
 *
 * Phase 4B: Atomically invalidates deterministic match results for the resume
 * upon successful skill replacement.
 */
@Service
public class SkillPersistenceService {

    private final ResumeSkillRepository resumeSkillRepository;
    private final MatchResultRepository matchResultRepository;

    public SkillPersistenceService(ResumeSkillRepository resumeSkillRepository,
                                   MatchResultRepository matchResultRepository) {
        this.resumeSkillRepository = resumeSkillRepository;
        this.matchResultRepository = matchResultRepository;
    }

    @Transactional
    public void replaceSkills(Resume resume, List<ExtractedSkillCandidate> candidates) {
        resumeSkillRepository.deleteByResumeId(resume.getId());
        resumeSkillRepository.flush();

        for (ExtractedSkillCandidate candidate : candidates) {
            ResumeSkill resumeSkill = new ResumeSkill(
                    resume,
                    candidate.skill(),
                    candidate.confidence(),
                    candidate.contextSnippet(),
                    candidate.matchedText(),
                    candidate.method().name()
            );
            resumeSkillRepository.save(resumeSkill);
        }
        resumeSkillRepository.flush();

        // Phase 4B: Atomically invalidate existing match results for this resume
        if (resume.getId() != null) {
            matchResultRepository.markStaleByResumeId(resume.getId());
        }
    }
}
