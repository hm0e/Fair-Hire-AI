package com.fairhire.models;

import com.fairhire.models.enums.AnnotationStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "annotations", uniqueConstraints = {
    @UniqueConstraint(name = "uq_annotations_record_annotator",
                      columnNames = {"dataset_record_id", "annotator_id"})
})
public class Annotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_record_id", nullable = false)
    private DatasetRecord datasetRecord;

    @Column(name = "annotator_id", nullable = false, length = 100)
    private String annotatorId;

    @Column(name = "relevance_score", nullable = false)
    private Integer relevanceScore;

    @Column(name = "is_match", nullable = false)
    private Boolean isMatch;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "exclusionary_phrases_labeled")
    private String exclusionaryPhrasesLabeled = "[]";

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal confidence = new BigDecimal("1.00");

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AnnotationStatus status = AnnotationStatus.SUBMITTED;

    @Column(name = "is_consensus", nullable = false)
    private Boolean isConsensus = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Annotation() {}

    public Annotation(DatasetRecord datasetRecord, String annotatorId, Integer relevanceScore,
                      Boolean isMatch, BigDecimal confidence, AnnotationStatus status) {
        this.datasetRecord = datasetRecord;
        this.annotatorId = annotatorId;
        this.relevanceScore = relevanceScore;
        this.isMatch = isMatch;
        this.confidence = confidence != null ? confidence : new BigDecimal("1.00");
        this.status = status != null ? status : AnnotationStatus.SUBMITTED;
        this.isConsensus = false;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (confidence == null) confidence = new BigDecimal("1.00");
        if (status == null) status = AnnotationStatus.SUBMITTED;
        if (isConsensus == null) isConsensus = false;
        if (exclusionaryPhrasesLabeled == null) exclusionaryPhrasesLabeled = "[]";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DatasetRecord getDatasetRecord() { return datasetRecord; }
    public void setDatasetRecord(DatasetRecord datasetRecord) { this.datasetRecord = datasetRecord; }

    public String getAnnotatorId() { return annotatorId; }
    public void setAnnotatorId(String annotatorId) { this.annotatorId = annotatorId; }

    public Integer getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(Integer relevanceScore) { this.relevanceScore = relevanceScore; }

    public Boolean getIsMatch() { return isMatch; }
    public void setIsMatch(Boolean isMatch) { this.isMatch = isMatch; }

    public String getExclusionaryPhrasesLabeled() { return exclusionaryPhrasesLabeled; }
    public void setExclusionaryPhrasesLabeled(String exclusionaryPhrasesLabeled) { this.exclusionaryPhrasesLabeled = exclusionaryPhrasesLabeled; }

    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }

    public AnnotationStatus getStatus() { return status; }
    public void setStatus(AnnotationStatus status) { this.status = status; }

    public Boolean getIsConsensus() { return isConsensus; }
    public void setIsConsensus(Boolean isConsensus) { this.isConsensus = isConsensus; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
