package com.fairhire.repositories;

import com.fairhire.models.ExperimentRun;
import com.fairhire.models.enums.ExperimentMethod;
import com.fairhire.models.enums.ExperimentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExperimentRunRepository extends JpaRepository<ExperimentRun, UUID> {
    List<ExperimentRun> findAllByOrderByStartedAtDesc();
    List<ExperimentRun> findByDatasetVersionId(Long datasetVersionId);
    List<ExperimentRun> findByMethod(ExperimentMethod method);
    List<ExperimentRun> findByStatus(ExperimentStatus status);
}
