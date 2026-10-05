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

    // Compatibility method
    default List<MatchResult> findByJobIdOrderByNormalRankAsc(Long jobId) {
        return findByJobIdOrderByRankInPoolAsc(jobId);
    }
}
