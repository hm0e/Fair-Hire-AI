package com.fairhire.repositories;

import com.fairhire.models.ExperimentMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExperimentMetricRepository extends JpaRepository<ExperimentMetric, Long> {
    Optional<ExperimentMetric> findByExperimentRunId(UUID experimentRunId);
}
