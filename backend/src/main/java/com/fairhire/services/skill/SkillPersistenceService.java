package com.fairhire.services.skill;

import com.fairhire.models.Resume;
import com.fairhire.models.ResumeSkill;
import com.fairhire.repositories.ResumeSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Isolated transaction service for replacing skills of a resume.
 * Executes skill deletion and persistence in an isolated transaction
 * so any failure rolls back partial skills cleanly without affecting
 * parent lifecycle status recording.
 */
@Service
public class SkillPersistenceService {

    private final ResumeSkillRepository resumeSkillRepository;

    public SkillPersistenceService(ResumeSkillRepository resumeSkillRepository) {
        this.resumeSkillRepository = resumeSkillRepository;
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
    }
}
