package com.fairhire.models;

import com.fairhire.models.enums.MatchingMethod;
import com.fairhire.models.enums.ScreeningMode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "match_results", uniqueConstraints = {
    @UniqueConstraint(name = "uq_match_results_job_resume_mode_method",
                      columnNames = {"job_id", "resume_id", "screening_mode", "matching_method"})
})
public class MatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Enumerated(EnumType.STRING)
    @Column(name = "screening_mode", nullable = false, length = 20)
    private ScreeningMode screeningMode = ScreeningMode.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "matching_method", nullable = false, length = 30)
    private MatchingMethod matchingMethod = MatchingMethod.HYBRID;

    @Column(name = "keyword_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal keywordScore = BigDecimal.ZERO;

    @Column(name = "skill_jaccard_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal skillJaccardScore = BigDecimal.ZERO;

    @Column(name = "semantic_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal semanticScore = BigDecimal.ZERO;

    @Column(name = "keyword_weight", nullable = false, precision = 4, scale = 3)
    private BigDecimal keywordWeight = new BigDecimal("0.300");

    @Column(name = "semantic_weight", nullable = false, precision = 4, scale = 3)
    private BigDecimal semanticWeight = new BigDecimal("0.700");

    @Column(name = "final_composite_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal finalCompositeScore = BigDecimal.ZERO;

    @Column(name = "rank_in_pool", nullable = false)
    private Integer rankInPool = 1;

    @Column(name = "matched_skills_count", nullable = false)
    private Integer matchedSkillsCount = 0;

    @Column(name = "missing_skills_count", nullable = false)
    private Integer missingSkillsCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_skills")
    private String missingSkills = "[]";

    @Column(name = "model_version", nullable = false, length = 100)
    private String modelVersion = "all-MiniLM-L6-v2";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public MatchResult() {}

    public MatchResult(Job job, Candidate candidate, Resume resume, ScreeningMode screeningMode,
                       MatchingMethod matchingMethod, BigDecimal keywordScore, BigDecimal skillJaccardScore,
                       BigDecimal semanticScore, BigDecimal keywordWeight, BigDecimal semanticWeight,
                       BigDecimal finalCompositeScore, Integer rankInPool) {
        this.job = job;
        this.candidate = candidate;
        this.resume = resume;
        this.screeningMode = screeningMode != null ? screeningMode : ScreeningMode.NORMAL;
        this.matchingMethod = matchingMethod != null ? matchingMethod : MatchingMethod.HYBRID;
        this.keywordScore = keywordScore != null ? keywordScore : BigDecimal.ZERO;
        this.skillJaccardScore = skillJaccardScore != null ? skillJaccardScore : BigDecimal.ZERO;
        this.semanticScore = semanticScore != null ? semanticScore : BigDecimal.ZERO;
        this.keywordWeight = keywordWeight != null ? keywordWeight : new BigDecimal("0.300");
        this.semanticWeight = semanticWeight != null ? semanticWeight : new BigDecimal("0.700");
        this.finalCompositeScore = finalCompositeScore != null ? finalCompositeScore : BigDecimal.ZERO;
        this.rankInPool = rankInPool != null ? rankInPool : 1;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (keywordScore == null) keywordScore = BigDecimal.ZERO;
        if (skillJaccardScore == null) skillJaccardScore = BigDecimal.ZERO;
        if (semanticScore == null) semanticScore = BigDecimal.ZERO;
        if (keywordWeight == null) keywordWeight = new BigDecimal("0.300");
        if (semanticWeight == null) semanticWeight = new BigDecimal("0.700");
        if (finalCompositeScore == null) finalCompositeScore = BigDecimal.ZERO;
        if (rankInPool == null) rankInPool = 1;
        if (matchedSkillsCount == null) matchedSkillsCount = 0;
        if (missingSkillsCount == null) missingSkillsCount = 0;
        if (missingSkills == null) missingSkills = "[]";
        if (modelVersion == null) modelVersion = "all-MiniLM-L6-v2";
        if (screeningMode == null) screeningMode = ScreeningMode.NORMAL;
        if (matchingMethod == null) matchingMethod = MatchingMethod.HYBRID;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public Candidate getCandidate() { return candidate; }
    public void setCandidate(Candidate candidate) { this.candidate = candidate; }

    public Resume getResume() { return resume; }
    public void setResume(Resume resume) { this.resume = resume; }

    public ScreeningMode getScreeningMode() { return screeningMode; }
    public void setScreeningMode(ScreeningMode screeningMode) { this.screeningMode = screeningMode; }

    public MatchingMethod getMatchingMethod() { return matchingMethod; }
    public void setMatchingMethod(MatchingMethod matchingMethod) { this.matchingMethod = matchingMethod; }

    public BigDecimal getKeywordScore() { return keywordScore; }
    public void setKeywordScore(BigDecimal keywordScore) { this.keywordScore = keywordScore; }

    public BigDecimal getSkillJaccardScore() { return skillJaccardScore; }
    public void setSkillJaccardScore(BigDecimal skillJaccardScore) { this.skillJaccardScore = skillJaccardScore; }

    public BigDecimal getSemanticScore() { return semanticScore; }
    public void setSemanticScore(BigDecimal semanticScore) { this.semanticScore = semanticScore; }

    public BigDecimal getKeywordWeight() { return keywordWeight; }
    public void setKeywordWeight(BigDecimal keywordWeight) { this.keywordWeight = keywordWeight; }

    public BigDecimal getSemanticWeight() { return semanticWeight; }
    public void setSemanticWeight(BigDecimal semanticWeight) { this.semanticWeight = semanticWeight; }

    public BigDecimal getFinalCompositeScore() { return finalCompositeScore; }
    public void setFinalCompositeScore(BigDecimal finalCompositeScore) { this.finalCompositeScore = finalCompositeScore; }

    // Convenience accessors
    public Double getFinalScore() {
        return finalCompositeScore != null ? finalCompositeScore.doubleValue() : 0.0;
    }
    public void setFinalScore(Double score) {
        this.finalCompositeScore = score != null ? BigDecimal.valueOf(score) : BigDecimal.ZERO;
    }

    public Integer getRankInPool() { return rankInPool; }
    public void setRankInPool(Integer rankInPool) { this.rankInPool = rankInPool; }

    public Integer getMatchedSkillsCount() { return matchedSkillsCount; }
    public void setMatchedSkillsCount(Integer matchedSkillsCount) { this.matchedSkillsCount = matchedSkillsCount; }

    public Integer getMissingSkillsCount() { return missingSkillsCount; }
    public void setMissingSkillsCount(Integer missingSkillsCount) { this.missingSkillsCount = missingSkillsCount; }

    public String getMissingSkills() { return missingSkills; }
    public void setMissingSkills(String missingSkills) { this.missingSkills = missingSkills; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
