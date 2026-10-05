package com.fairhire.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "experiment_results", uniqueConstraints = {
    @UniqueConstraint(name = "uq_experiment_results_run_record",
                      columnNames = {"experiment_run_id", "dataset_record_id"})
})
public class ExperimentResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_run_id", nullable = false)
    private ExperimentRun experimentRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_record_id", nullable = false)
    private DatasetRecord datasetRecord;

    @Column(name = "normal_score", nullable = false, precision = 6, scale = 5)
    private BigDecimal normalScore;

    @Column(name = "blind_score", nullable = false, precision = 6, scale = 5)
    private BigDecimal blindScore;

    @Column(name = "normal_rank", nullable = false)
    private Integer normalRank;

    @Column(name = "blind_rank", nullable = false)
    private Integer blindRank;

    /**
     * PostgreSQL Generated Column: GENERATED ALWAYS AS (ABS(normal_rank - blind_rank)) STORED
     */
    @Column(name = "rank_change", insertable = false, updatable = false)
    private Integer rankChange;

    @Column(name = "ground_truth_relevance")
    private Integer groundTruthRelevance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ExperimentResult() {}

    public ExperimentResult(ExperimentRun experimentRun, DatasetRecord datasetRecord,
                            BigDecimal normalScore, BigDecimal blindScore,
                            Integer normalRank, Integer blindRank, Integer groundTruthRelevance) {
        this.experimentRun = experimentRun;
        this.datasetRecord = datasetRecord;
        this.normalScore = normalScore;
        this.blindScore = blindScore;
        this.normalRank = normalRank;
        this.blindRank = blindRank;
        this.groundTruthRelevance = groundTruthRelevance;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ExperimentRun getExperimentRun() { return experimentRun; }
    public void setExperimentRun(ExperimentRun experimentRun) { this.experimentRun = experimentRun; }

    public DatasetRecord getDatasetRecord() { return datasetRecord; }
    public void setDatasetRecord(DatasetRecord datasetRecord) { this.datasetRecord = datasetRecord; }

    public BigDecimal getNormalScore() { return normalScore; }
    public void setNormalScore(BigDecimal normalScore) { this.normalScore = normalScore; }

    public BigDecimal getBlindScore() { return blindScore; }
    public void setBlindScore(BigDecimal blindScore) { this.blindScore = blindScore; }

    public Integer getNormalRank() { return normalRank; }
    public void setNormalRank(Integer normalRank) { this.normalRank = normalRank; }

    public Integer getBlindRank() { return blindRank; }
    public void setBlindRank(Integer blindRank) { this.blindRank = blindRank; }

    public Integer getRankChange() { return rankChange; }

    public Integer getGroundTruthRelevance() { return groundTruthRelevance; }
    public void setGroundTruthRelevance(Integer groundTruthRelevance) { this.groundTruthRelevance = groundTruthRelevance; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
