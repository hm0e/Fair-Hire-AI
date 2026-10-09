package com.fairhire.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class MatchSkillDetailDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("job_skill_id")
    @com.fasterxml.jackson.annotation.JsonAlias("jobSkillId")
    private Long jobSkillId;

    @JsonProperty("skill_id")
    @com.fasterxml.jackson.annotation.JsonAlias("skillId")
    private Long skillId;

    @JsonProperty("skill_name")
    @com.fasterxml.jackson.annotation.JsonAlias("skillName")
    private String skillName;

    @JsonProperty("necessity")
    private String necessity;

    @JsonProperty("is_matched")
    @com.fasterxml.jackson.annotation.JsonAlias("matched")
    private Boolean isMatched;

    @JsonProperty("matched")
    public Boolean getMatched() { return isMatched; }

    @JsonProperty("candidate_confidence")
    @com.fasterxml.jackson.annotation.JsonAlias("candidateConfidence")
    private BigDecimal candidateConfidence;

    @JsonProperty("candidate_matched_text")
    @com.fasterxml.jackson.annotation.JsonAlias({"candidateMatchedText", "matchedText", "matched_text"})
    private String candidateMatchedText;

    @JsonProperty("matched_text")
    public String getMatchedText() { return candidateMatchedText; }

    @JsonProperty("candidate_context_snippet")
    @com.fasterxml.jackson.annotation.JsonAlias({"candidateContextSnippet", "contextSnippet", "context_snippet"})
    private String candidateContextSnippet;

    @JsonProperty("context_snippet")
    public String getContextSnippet() { return candidateContextSnippet; }

    public MatchSkillDetailDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getJobSkillId() { return jobSkillId; }
    public void setJobSkillId(Long jobSkillId) { this.jobSkillId = jobSkillId; }

    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public String getNecessity() { return necessity; }
    public void setNecessity(String necessity) { this.necessity = necessity; }

    public Boolean getIsMatched() { return isMatched; }
    public void setIsMatched(Boolean isMatched) { this.isMatched = isMatched; }

    public BigDecimal getCandidateConfidence() { return candidateConfidence; }
    public void setCandidateConfidence(BigDecimal candidateConfidence) { this.candidateConfidence = candidateConfidence; }

    public String getCandidateMatchedText() { return candidateMatchedText; }
    public void setCandidateMatchedText(String candidateMatchedText) { this.candidateMatchedText = candidateMatchedText; }

    public String getCandidateContextSnippet() { return candidateContextSnippet; }
    public void setCandidateContextSnippet(String candidateContextSnippet) { this.candidateContextSnippet = candidateContextSnippet; }
}
