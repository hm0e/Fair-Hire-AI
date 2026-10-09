package com.fairhire.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

public class MatchResponseDto {

    @JsonProperty("match_id")
    private Long matchId;

    @JsonProperty("resume_id")
    private Long resumeId;

    @JsonProperty("candidate_id")
    private Long candidateId;

    @JsonProperty("overall_score")
    private BigDecimal overallScore;

    @JsonProperty("required_skill_coverage")
    private BigDecimal requiredSkillCoverage;

    @JsonProperty("preferred_skill_coverage")
    private BigDecimal preferredSkillCoverage;

    @JsonProperty("required_skills_matched")
    private Integer requiredSkillsMatched;

    @JsonProperty("required_skills_total")
    private Integer requiredSkillsTotal;

    @JsonProperty("preferred_skills_matched")
    private Integer preferredSkillsMatched;

    @JsonProperty("preferred_skills_total")
    private Integer preferredSkillsTotal;

    @JsonProperty("is_stale")
    private Boolean isStale;

    @JsonProperty("scored_at")
    private Instant scoredAt;

    public MatchResponseDto() {}

    public Long getMatchId() { return matchId; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }

    public Long getResumeId() { return resumeId; }
    public void setResumeId(Long resumeId) { this.resumeId = resumeId; }

    public Long getCandidateId() { return candidateId; }
    public void setCandidateId(Long candidateId) { this.candidateId = candidateId; }

    public BigDecimal getOverallScore() { return overallScore; }
    public void setOverallScore(BigDecimal overallScore) { this.overallScore = overallScore; }

    public BigDecimal getRequiredSkillCoverage() { return requiredSkillCoverage; }
    public void setRequiredSkillCoverage(BigDecimal requiredSkillCoverage) { this.requiredSkillCoverage = requiredSkillCoverage; }

    public BigDecimal getPreferredSkillCoverage() { return preferredSkillCoverage; }
    public void setPreferredSkillCoverage(BigDecimal preferredSkillCoverage) { this.preferredSkillCoverage = preferredSkillCoverage; }

    public Integer getRequiredSkillsMatched() { return requiredSkillsMatched; }
    public void setRequiredSkillsMatched(Integer requiredSkillsMatched) { this.requiredSkillsMatched = requiredSkillsMatched; }

    public Integer getRequiredSkillsTotal() { return requiredSkillsTotal; }
    public void setRequiredSkillsTotal(Integer requiredSkillsTotal) { this.requiredSkillsTotal = requiredSkillsTotal; }

    public Integer getPreferredSkillsMatched() { return preferredSkillsMatched; }
    public void setPreferredSkillsMatched(Integer preferredSkillsMatched) { this.preferredSkillsMatched = preferredSkillsMatched; }

    public Integer getPreferredSkillsTotal() { return preferredSkillsTotal; }
    public void setPreferredSkillsTotal(Integer preferredSkillsTotal) { this.preferredSkillsTotal = preferredSkillsTotal; }

    public Boolean getIsStale() { return isStale; }
    public void setIsStale(Boolean isStale) { this.isStale = isStale; }

    public Instant getScoredAt() { return scoredAt; }
    public void setScoredAt(Instant scoredAt) { this.scoredAt = scoredAt; }
}
