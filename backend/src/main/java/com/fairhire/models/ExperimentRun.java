package com.fairhire.models;

import com.fairhire.models.enums.ExperimentMethod;
import com.fairhire.models.enums.ExperimentStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "experiment_runs")
public class ExperimentRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "run_name", nullable = false, length = 150)
    private String runName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_version_id", nullable = false)
    private DatasetVersion datasetVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "executor_id")
    private User executor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ExperimentMethod method = ExperimentMethod.HYBRID;

    @Column(name = "embedding_model", nullable = false, length = 100)
    private String embeddingModel = "all-MiniLM-L6-v2";

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion = "1.0.0";

    @Column(name = "preprocessing_version", nullable = false, length = 50)
    private String preprocessingVersion = "v1.0-standard";

    @Column(name = "anonymization_version", nullable = false, length = 50)
    private String anonymizationVersion = "v1.0-ruleset";

    @Column(name = "git_commit_hash", length = 40)
    private String gitCommitHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String hyperparameters = "{}";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExperimentStatus status = ExperimentStatus.STARTED;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @OneToMany(mappedBy = "experimentRun", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExperimentResult> results = new ArrayList<>();

    @OneToOne(mappedBy = "experimentRun", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ExperimentMetric metric;

    public ExperimentRun() {}

    public ExperimentRun(String runName, DatasetVersion datasetVersion, User executor, ExperimentMethod method) {
        this.runName = runName;
        this.datasetVersion = datasetVersion;
        this.executor = executor;
        this.method = method != null ? method : ExperimentMethod.HYBRID;
        this.status = ExperimentStatus.STARTED;
        this.startedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) startedAt = Instant.now();
        if (embeddingModel == null) embeddingModel = "all-MiniLM-L6-v2";
        if (modelVersion == null) modelVersion = "1.0.0";
        if (preprocessingVersion == null) preprocessingVersion = "v1.0-standard";
        if (anonymizationVersion == null) anonymizationVersion = "v1.0-ruleset";
        if (hyperparameters == null) hyperparameters = "{}";
        if (status == null) status = ExperimentStatus.STARTED;
        if (method == null) method = ExperimentMethod.HYBRID;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getRunName() { return runName; }
    public void setRunName(String runName) { this.runName = runName; }

    public DatasetVersion getDatasetVersion() { return datasetVersion; }
    public void setDatasetVersion(DatasetVersion datasetVersion) { this.datasetVersion = datasetVersion; }

    public User getExecutor() { return executor; }
    public void setExecutor(User executor) { this.executor = executor; }

    public ExperimentMethod getMethod() { return method; }
    public void setMethod(ExperimentMethod method) { this.method = method; }

    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public String getPreprocessingVersion() { return preprocessingVersion; }
    public void setPreprocessingVersion(String preprocessingVersion) { this.preprocessingVersion = preprocessingVersion; }

    public String getAnonymizationVersion() { return anonymizationVersion; }
    public void setAnonymizationVersion(String anonymizationVersion) { this.anonymizationVersion = anonymizationVersion; }

    public String getGitCommitHash() { return gitCommitHash; }
    public void setGitCommitHash(String gitCommitHash) { this.gitCommitHash = gitCommitHash; }

    public String getHyperparameters() { return hyperparameters; }
    public void setHyperparameters(String hyperparameters) { this.hyperparameters = hyperparameters; }

    public ExperimentStatus getStatus() { return status; }
    public void setStatus(ExperimentStatus status) { this.status = status; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public List<ExperimentResult> getResults() { return results; }
    public void setResults(List<ExperimentResult> results) { this.results = results; }

    public ExperimentMetric getMetric() { return metric; }
    public void setMetric(ExperimentMetric metric) { this.metric = metric; }
}
