package com.fairhire.repositories;

import com.fairhire.models.DatasetRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatasetRecordRepository extends JpaRepository<DatasetRecord, Long> {
    List<DatasetRecord> findByDatasetVersionId(Long datasetVersionId);
    Optional<DatasetRecord> findByDatasetVersionIdAndRecordIdentifier(Long datasetVersionId, String recordIdentifier);
}
