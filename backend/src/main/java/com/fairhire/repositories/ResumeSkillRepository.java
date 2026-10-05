package com.fairhire.repositories;

import com.fairhire.models.ResumeSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeSkillRepository extends JpaRepository<ResumeSkill, Long> {
    List<ResumeSkill> findByResumeId(Long resumeId);
    Optional<ResumeSkill> findByResumeIdAndSkillId(Long resumeId, Long skillId);

    @org.springframework.transaction.annotation.Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ResumeSkill rs WHERE rs.resume.id = :resumeId")
    void deleteByResumeId(@Param("resumeId") Long resumeId);
}
