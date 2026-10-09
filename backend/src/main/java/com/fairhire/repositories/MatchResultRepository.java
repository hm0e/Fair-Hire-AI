package com.fairhire.repositories;

import com.fairhire.models.MatchResult;
import com.fairhire.models.enums.MatchingMethod;
import com.fairhire.models.enums.ScreeningMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {
    List<MatchResult> findByJobIdOrderByRankInPoolAsc(Long jobId);

    List<MatchResult> findByJobIdAndScreeningModeOrderByRankInPoolAsc(Long jobId, ScreeningMode screeningMode);

    Optional<MatchResult> findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
            Long jobId, Long resumeId, ScreeningMode screeningMode, MatchingMethod matchingMethod);

    List<MatchResult> findByJobIdAndResumeId(Long jobId, Long resumeId);

    List<MatchResult> findByJobIdAndMatchingMethodOrderByOverallScoreDesc(Long jobId, MatchingMethod matchingMethod);

    List<MatchResult> findByJobIdAndMatchingMethod(Long jobId, MatchingMethod matchingMethod);

    Optional<MatchResult> findByJobIdAndResumeIdAndMatchingMethod(Long jobId, Long resumeId, MatchingMethod matchingMethod);

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("UPDATE MatchResult m SET m.isStale = true WHERE m.job.id = :jobId")
    void markStaleByJobId(@org.springframework.data.repository.query.Param("jobId") Long jobId);

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("UPDATE MatchResult m SET m.isStale = true WHERE m.resume.id = :resumeId")
    void markStaleByResumeId(@org.springframework.data.repository.query.Param("resumeId") Long resumeId);

    // Compatibility method
    default List<MatchResult> findByJobIdOrderByNormalRankAsc(Long jobId) {
        return findByJobIdOrderByRankInPoolAsc(jobId);
    }
}
