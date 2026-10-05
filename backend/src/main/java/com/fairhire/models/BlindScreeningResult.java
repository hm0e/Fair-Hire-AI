package com.fairhire.models;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "blind_screening_results")
public class BlindScreeningResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false, unique = true)
    private Resume resume;

    @Column(name = "anonymized_text", nullable = false, columnDefinition = "TEXT")
    private String anonymizedText;

    @Column(name = "redactions_count", nullable = false)
    private Integer redactionsCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "redactions_log", nullable = false)
    private String redactionsLog = "[]";

    @Column(name = "anonymization_ruleset_version", nullable = false, length = 50)
    private String anonymizationRulesetVersion = "v1.0";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public BlindScreeningResult() {}

    public BlindScreeningResult(Resume resume, String anonymizedText, Integer redactionsCount,
                                String redactionsLog, String anonymizationRulesetVersion) {
        this.resume = resume;
        this.anonymizedText = anonymizedText;
        this.redactionsCount = redactionsCount != null ? redactionsCount : 0;
        this.redactionsLog = redactionsLog != null ? redactionsLog : "[]";
        this.anonymizationRulesetVersion = anonymizationRulesetVersion != null ? anonymizationRulesetVersion : "v1.0";
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (redactionsCount == null) redactionsCount = 0;
        if (redactionsLog == null) redactionsLog = "[]";
        if (anonymizationRulesetVersion == null) anonymizationRulesetVersion = "v1.0";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Resume getResume() { return resume; }
    public void setResume(Resume resume) { this.resume = resume; }

    public String getAnonymizedText() { return anonymizedText; }
    public void setAnonymizedText(String anonymizedText) { this.anonymizedText = anonymizedText; }

    public Integer getRedactionsCount() { return redactionsCount; }
    public void setRedactionsCount(Integer redactionsCount) { this.redactionsCount = redactionsCount; }

    public String getRedactionsLog() { return redactionsLog; }
    public void setRedactionsLog(String redactionsLog) { this.redactionsLog = redactionsLog; }

    public String getAnonymizationRulesetVersion() { return anonymizationRulesetVersion; }
    public void setAnonymizationRulesetVersion(String anonymizationRulesetVersion) { this.anonymizationRulesetVersion = anonymizationRulesetVersion; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
