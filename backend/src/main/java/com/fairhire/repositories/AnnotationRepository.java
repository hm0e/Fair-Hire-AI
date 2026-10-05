package com.fairhire.repositories;

import com.fairhire.models.Annotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnnotationRepository extends JpaRepository<Annotation, Long> {
    List<Annotation> findByDatasetRecordId(Long datasetRecordId);
    Optional<Annotation> findByDatasetRecordIdAndAnnotatorId(Long datasetRecordId, String annotatorId);
}
