package com.fairhire.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "experiment_metrics")
public class ExperimentMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_run_id", nullable = false, unique = true)
    private ExperimentRun experimentRun;

    @Column(name = "spearman_rho", nullable = false, precision = 6, scale = 5)
    private BigDecimal spearmanRho;

    @Column(name = "mean_rank_shift", nullable = false, precision = 6, scale = 3)
    private BigDecimal meanRankShift;

    @Column(name = "candidates_with_rank_change_count", nullable = false)
    private Integer candidatesWithRankChangeCount;

    @Column(name = "precision_at_k", nullable = false, precision = 5, scale = 4)
    private BigDecimal precisionAtK;

    @Column(name = "recall_at_k", nullable = false, precision = 5, scale = 4)
    private BigDecimal recallAtK;

    @Column(name = "ndcg_at_k", nullable = false, precision = 5, scale = 4)
    private BigDecimal ndcgAtK;

    @Column(name = "f1_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal f1Score;

    @Column(name = "jd_bias_score_before", nullable = false, precision = 5, scale = 4)
    private BigDecimal jdBiasScoreBefore;

    @Column(name = "jd_bias_score_after", nullable = false, precision = 5, scale = 4)
    private BigDecimal jdBiasScoreAfter;

    @Column(name = "bias_reduction_delta", nullable = false, precision = 5, scale = 4)
    private BigDecimal biasReductionDelta;

    @Column(name = "eval_latency_avg_ms", nullable = false, precision = 8, scale = 2)
    private BigDecimal evalLatencyAvgMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ExperimentMetric() {}

    public ExperimentMetric(ExperimentRun experimentRun, BigDecimal spearmanRho, BigDecimal meanRankShift,
                            Integer candidatesWithRankChangeCount, BigDecimal precisionAtK, BigDecimal recallAtK,
                            BigDecimal ndcgAtK, BigDecimal f1Score, BigDecimal jdBiasScoreBefore,
                            BigDecimal jdBiasScoreAfter, BigDecimal biasReductionDelta, BigDecimal evalLatencyAvgMs) {
        this.experimentRun = experimentRun;
        this.spearmanRho = spearmanRho;
        this.meanRankShift = meanRankShift;
        this.candidatesWithRankChangeCount = candidatesWithRankChangeCount;
        this.precisionAtK = precisionAtK;
        this.recallAtK = recallAtK;
        this.ndcgAtK = ndcgAtK;
        this.f1Score = f1Score;
        this.jdBiasScoreBefore = jdBiasScoreBefore;
        this.jdBiasScoreAfter = jdBiasScoreAfter;
        this.biasReductionDelta = biasReductionDelta;
        this.evalLatencyAvgMs = evalLatencyAvgMs;
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

    public BigDecimal getSpearmanRho() { return spearmanRho; }
    public void setSpearmanRho(BigDecimal spearmanRho) { this.spearmanRho = spearmanRho; }

    public BigDecimal getMeanRankShift() { return meanRankShift; }
    public void setMeanRankShift(BigDecimal meanRankShift) { this.meanRankShift = meanRankShift; }

    public Integer getCandidatesWithRankChangeCount() { return candidatesWithRankChangeCount; }
    public void setCandidatesWithRankChangeCount(Integer candidatesWithRankChangeCount) { this.candidatesWithRankChangeCount = candidatesWithRankChangeCount; }

    public BigDecimal getPrecisionAtK() { return precisionAtK; }
    public void setPrecisionAtK(BigDecimal precisionAtK) { this.precisionAtK = precisionAtK; }

    public BigDecimal getRecallAtK() { return recallAtK; }
    public void setRecallAtK(BigDecimal recallAtK) { this.recallAtK = recallAtK; }

    public BigDecimal getNdcgAtK() { return ndcgAtK; }
    public void setNdcgAtK(BigDecimal ndcgAtK) { this.ndcgAtK = ndcgAtK; }

    public BigDecimal getF1Score() { return f1Score; }
    public void setF1Score(BigDecimal f1Score) { this.f1Score = f1Score; }

    public BigDecimal getJdBiasScoreBefore() { return jdBiasScoreBefore; }
    public void setJdBiasScoreBefore(BigDecimal jdBiasScoreBefore) { this.jdBiasScoreBefore = jdBiasScoreBefore; }

    public BigDecimal getJdBiasScoreAfter() { return jdBiasScoreAfter; }
    public void setJdBiasScoreAfter(BigDecimal jdBiasScoreAfter) { this.jdBiasScoreAfter = jdBiasScoreAfter; }

    public BigDecimal getBiasReductionDelta() { return biasReductionDelta; }
    public void setBiasReductionDelta(BigDecimal biasReductionDelta) { this.biasReductionDelta = biasReductionDelta; }

    public BigDecimal getEvalLatencyAvgMs() { return evalLatencyAvgMs; }
    public void setEvalLatencyAvgMs(BigDecimal evalLatencyAvgMs) { this.evalLatencyAvgMs = evalLatencyAvgMs; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
