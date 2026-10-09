package com.fairhire.models;

import com.fairhire.models.enums.RequirementNecessity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "match_skill_details", uniqueConstraints = {
    @UniqueConstraint(name = "uq_match_skill_details_result_jobskill", columnNames = {"match_result_id", "job_skill_id"})
})
public class MatchSkillDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_result_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private MatchResult matchResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_skill_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private JobSkill jobSkill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RequirementNecessity necessity;

    @Column(name = "is_matched", nullable = false)
    private Boolean isMatched;

    @Column(name = "candidate_confidence", precision = 4, scale = 3)
    private BigDecimal candidateConfidence;

    @Column(name = "candidate_matched_text", length = 100)
    private String candidateMatchedText;

    @Column(name = "candidate_context_snippet", length = 300)
    private String candidateContextSnippet;

    public MatchSkillDetail() {}

    public MatchSkillDetail(MatchResult matchResult, JobSkill jobSkill, Skill skill,
                            RequirementNecessity necessity, Boolean isMatched,
                            BigDecimal candidateConfidence, String candidateMatchedText,
                            String candidateContextSnippet) {
        this.matchResult = matchResult;
        this.jobSkill = jobSkill;
        this.skill = skill;
        this.necessity = necessity;
        this.isMatched = isMatched != null ? isMatched : false;
        this.candidateConfidence = candidateConfidence;
        this.candidateMatchedText = candidateMatchedText;
        this.candidateContextSnippet = candidateContextSnippet;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MatchResult getMatchResult() { return matchResult; }
    public void setMatchResult(MatchResult matchResult) { this.matchResult = matchResult; }

    public JobSkill getJobSkill() { return jobSkill; }
    public void setJobSkill(JobSkill jobSkill) { this.jobSkill = jobSkill; }

    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }

    public RequirementNecessity getNecessity() { return necessity; }
    public void setNecessity(RequirementNecessity necessity) { this.necessity = necessity; }

    public Boolean getIsMatched() { return isMatched; }
    public void setIsMatched(Boolean isMatched) { this.isMatched = isMatched; }

    public BigDecimal getCandidateConfidence() { return candidateConfidence; }
    public void setCandidateConfidence(BigDecimal candidateConfidence) { this.candidateConfidence = candidateConfidence; }

    public String getCandidateMatchedText() { return candidateMatchedText; }
    public void setCandidateMatchedText(String candidateMatchedText) { this.candidateMatchedText = candidateMatchedText; }

    public String getCandidateContextSnippet() { return candidateContextSnippet; }
    public void setCandidateContextSnippet(String candidateContextSnippet) { this.candidateContextSnippet = candidateContextSnippet; }
}
