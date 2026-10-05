package com.fairhire.models;

import com.fairhire.models.enums.BiasTargetVersion;
import com.fairhire.models.enums.GenderLean;
import com.fairhire.models.enums.ReadingLevel;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bias_reports")
public class BiasReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_version", nullable = false, length = 20)
    private BiasTargetVersion targetVersion = BiasTargetVersion.ORIGINAL;

    @Column(name = "gender_bias_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal genderBiasScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_lean", nullable = false, length = 30)
    private GenderLean genderLean = GenderLean.BALANCED;

    @Column(name = "age_bias_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal ageBiasScore;

    @Column(name = "flesch_reading_ease", nullable = false, precision = 5, scale = 2)
    private BigDecimal fleschReadingEase;

    @Enumerated(EnumType.STRING)
    @Column(name = "reading_level", nullable = false, length = 30)
    private ReadingLevel readingLevel = ReadingLevel.MODERATE;

    @Column(name = "total_flagged_terms", nullable = false)
    private Integer totalFlaggedTerms = 0;

    @Column(name = "masculine_terms_count", nullable = false)
    private Integer masculineTermsCount = 0;

    @Column(name = "feminine_terms_count", nullable = false)
    private Integer feminineTermsCount = 0;

    @Column(name = "age_terms_count", nullable = false)
    private Integer ageTermsCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "biasReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RewriteSuggestion> rewriteSuggestions = new ArrayList<>();

    public BiasReport() {}

    public BiasReport(Job job, BiasTargetVersion targetVersion, BigDecimal genderBiasScore,
                      GenderLean genderLean, BigDecimal ageBiasScore, BigDecimal fleschReadingEase,
                      ReadingLevel readingLevel) {
        this.job = job;
        this.targetVersion = targetVersion != null ? targetVersion : BiasTargetVersion.ORIGINAL;
        this.genderBiasScore = genderBiasScore != null ? genderBiasScore : BigDecimal.ZERO;
        this.genderLean = genderLean != null ? genderLean : GenderLean.BALANCED;
        this.ageBiasScore = ageBiasScore != null ? ageBiasScore : BigDecimal.ZERO;
        this.fleschReadingEase = fleschReadingEase != null ? fleschReadingEase : BigDecimal.valueOf(60.0);
        this.readingLevel = readingLevel != null ? readingLevel : ReadingLevel.MODERATE;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (targetVersion == null) targetVersion = BiasTargetVersion.ORIGINAL;
        if (genderLean == null) genderLean = GenderLean.BALANCED;
        if (readingLevel == null) readingLevel = ReadingLevel.MODERATE;
        if (totalFlaggedTerms == null) totalFlaggedTerms = 0;
        if (masculineTermsCount == null) masculineTermsCount = 0;
        if (feminineTermsCount == null) feminineTermsCount = 0;
        if (ageTermsCount == null) ageTermsCount = 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public BiasTargetVersion getTargetVersion() { return targetVersion; }
    public void setTargetVersion(BiasTargetVersion targetVersion) { this.targetVersion = targetVersion; }

    public BigDecimal getGenderBiasScore() { return genderBiasScore; }
    public void setGenderBiasScore(BigDecimal genderBiasScore) { this.genderBiasScore = genderBiasScore; }

    public GenderLean getGenderLean() { return genderLean; }
    public void setGenderLean(GenderLean genderLean) { this.genderLean = genderLean; }

    public BigDecimal getAgeBiasScore() { return ageBiasScore; }
    public void setAgeBiasScore(BigDecimal ageBiasScore) { this.ageBiasScore = ageBiasScore; }

    public BigDecimal getFleschReadingEase() { return fleschReadingEase; }
    public void setFleschReadingEase(BigDecimal fleschReadingEase) { this.fleschReadingEase = fleschReadingEase; }

    public ReadingLevel getReadingLevel() { return readingLevel; }
    public void setReadingLevel(ReadingLevel readingLevel) { this.readingLevel = readingLevel; }

    public Integer getTotalFlaggedTerms() { return totalFlaggedTerms; }
    public void setTotalFlaggedTerms(Integer totalFlaggedTerms) { this.totalFlaggedTerms = totalFlaggedTerms; }

    public Integer getMasculineTermsCount() { return masculineTermsCount; }
    public void setMasculineTermsCount(Integer masculineTermsCount) { this.masculineTermsCount = masculineTermsCount; }

    public Integer getFeminineTermsCount() { return feminineTermsCount; }
    public void setFeminineTermsCount(Integer feminineTermsCount) { this.feminineTermsCount = feminineTermsCount; }

    public Integer getAgeTermsCount() { return ageTermsCount; }
    public void setAgeTermsCount(Integer ageTermsCount) { this.ageTermsCount = ageTermsCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public List<RewriteSuggestion> getRewriteSuggestions() { return rewriteSuggestions; }
    public void setRewriteSuggestions(List<RewriteSuggestion> rewriteSuggestions) { this.rewriteSuggestions = rewriteSuggestions; }
}
