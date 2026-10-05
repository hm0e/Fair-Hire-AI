package com.fairhire.repositories;

import com.fairhire.models.ExperimentResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExperimentResultRepository extends JpaRepository<ExperimentResult, Long> {
    List<ExperimentResult> findByExperimentRunIdOrderByNormalRankAsc(UUID experimentRunId);
    Optional<ExperimentResult> findByExperimentRunIdAndDatasetRecordId(UUID experimentRunId, Long datasetRecordId);
}
