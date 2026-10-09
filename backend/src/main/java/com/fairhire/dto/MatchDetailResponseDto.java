package com.fairhire.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MatchDetailResponseDto {

    @JsonProperty("match_id")
    @com.fasterxml.jackson.annotation.JsonAlias({"matchId", "matchResultId", "match_result_id"})
    private Long matchId;

    @JsonProperty("job_id")
    @com.fasterxml.jackson.annotation.JsonAlias("jobId")
    private Long jobId;

    @JsonProperty("resume_id")
    @com.fasterxml.jackson.annotation.JsonAlias("resumeId")
    private Long resumeId;

    @JsonProperty("candidate_id")
    @com.fasterxml.jackson.annotation.JsonAlias("candidateId")
    private Long candidateId;

    @JsonProperty("overall_score")
    @com.fasterxml.jackson.annotation.JsonAlias("overallScore")
    private BigDecimal overallScore;

    @JsonProperty("required_skill_coverage")
    @com.fasterxml.jackson.annotation.JsonAlias("requiredSkillCoverage")
    private BigDecimal requiredSkillCoverage;

    @JsonProperty("preferred_skill_coverage")
    @com.fasterxml.jackson.annotation.JsonAlias("preferredSkillCoverage")
    private BigDecimal preferredSkillCoverage;

    @JsonProperty("required_skills_matched")
    @com.fasterxml.jackson.annotation.JsonAlias("requiredSkillsMatched")
    private Integer requiredSkillsMatched;

    @JsonProperty("required_skills_total")
    @com.fasterxml.jackson.annotation.JsonAlias("requiredSkillsTotal")
    private Integer requiredSkillsTotal;

    @JsonProperty("preferred_skills_matched")
    @com.fasterxml.jackson.annotation.JsonAlias("preferredSkillsMatched")
    private Integer preferredSkillsMatched;

    @JsonProperty("preferred_skills_total")
    @com.fasterxml.jackson.annotation.JsonAlias("preferredSkillsTotal")
    private Integer preferredSkillsTotal;

    @JsonProperty("required_weight")
    @com.fasterxml.jackson.annotation.JsonAlias("requiredWeight")
    private BigDecimal requiredWeight;

    @JsonProperty("preferred_weight")
    @com.fasterxml.jackson.annotation.JsonAlias("preferredWeight")
    private BigDecimal preferredWeight;

    @JsonProperty("required_contribution")
    @com.fasterxml.jackson.annotation.JsonAlias("requiredContribution")
    private BigDecimal requiredContribution;

    @JsonProperty("preferred_contribution")
    @com.fasterxml.jackson.annotation.JsonAlias("preferredContribution")
    private BigDecimal preferredContribution;

    @JsonProperty("status")
    private String status;

    @JsonProperty("is_stale")
    @com.fasterxml.jackson.annotation.JsonAlias("isStale")
    private Boolean isStale;

    @JsonProperty("algorithm_version")
    @com.fasterxml.jackson.annotation.JsonAlias("algorithmVersion")
    private String algorithmVersion;

    @JsonProperty("matching_method")
    @com.fasterxml.jackson.annotation.JsonAlias("matchingMethod")
    private String matchingMethod;

    @JsonProperty("scored_at")
    @com.fasterxml.jackson.annotation.JsonAlias("scoredAt")
    private Instant scoredAt;

    @JsonProperty("required_skills")
    @com.fasterxml.jackson.annotation.JsonAlias("requiredSkills")
    private List<MatchSkillDetailDto> requiredSkills = new ArrayList<>();

    @JsonProperty("preferred_skills")
    @com.fasterxml.jackson.annotation.JsonAlias("preferredSkills")
    private List<MatchSkillDetailDto> preferredSkills = new ArrayList<>();

    public MatchDetailResponseDto() {}

    public Long getMatchId() { return matchId; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

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

    public String getAlgorithmVersion() { return algorithmVersion; }
    public void setAlgorithmVersion(String algorithmVersion) { this.algorithmVersion = algorithmVersion; }

    public String getMatchingMethod() { return matchingMethod; }
    public void setMatchingMethod(String matchingMethod) { this.matchingMethod = matchingMethod; }

    public Instant getScoredAt() { return scoredAt; }
    public void setScoredAt(Instant scoredAt) { this.scoredAt = scoredAt; }

    public List<MatchSkillDetailDto> getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(List<MatchSkillDetailDto> requiredSkills) { this.requiredSkills = requiredSkills; }

    public List<MatchSkillDetailDto> getPreferredSkills() { return preferredSkills; }
    public void setPreferredSkills(List<MatchSkillDetailDto> preferredSkills) { this.preferredSkills = preferredSkills; }

    @JsonProperty("match_result_id")
    public Long getMatchResultId() { return matchId; }

    public BigDecimal getRequiredWeight() { return requiredWeight; }
    public void setRequiredWeight(BigDecimal requiredWeight) { this.requiredWeight = requiredWeight; }

    public BigDecimal getPreferredWeight() { return preferredWeight; }
    public void setPreferredWeight(BigDecimal preferredWeight) { this.preferredWeight = preferredWeight; }

    public BigDecimal getRequiredContribution() { return requiredContribution; }
    public void setRequiredContribution(BigDecimal requiredContribution) { this.requiredContribution = requiredContribution; }

    public BigDecimal getPreferredContribution() { return preferredContribution; }
    public void setPreferredContribution(BigDecimal preferredContribution) { this.preferredContribution = preferredContribution; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
