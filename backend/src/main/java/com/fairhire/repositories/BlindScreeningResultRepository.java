package com.fairhire.repositories;

import com.fairhire.models.BlindScreeningResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlindScreeningResultRepository extends JpaRepository<BlindScreeningResult, Long> {
    Optional<BlindScreeningResult> findByResumeId(Long resumeId);
}
