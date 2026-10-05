package com.fairhire.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "resume_skills", uniqueConstraints = {
    @UniqueConstraint(name = "uq_resume_skills_resume_skill", columnNames = {"resume_id", "skill_id"})
})
public class ResumeSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(name = "extraction_confidence", nullable = false, precision = 4, scale = 3)
    private BigDecimal extractionConfidence = new BigDecimal("1.000");

    @Column(name = "context_snippet", length = 300)
    private String contextSnippet;

    @Column(name = "matched_text", length = 100)
    private String matchedText;

    @Column(name = "extraction_method", nullable = false, length = 50)
    private String extractionMethod = "EXACT_CANONICAL_MATCH";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ResumeSkill() {}

    public ResumeSkill(Resume resume, Skill skill) {
        this.resume = resume;
        this.skill = skill;
        this.extractionConfidence = new BigDecimal("1.000");
        this.extractionMethod = "EXACT_CANONICAL_MATCH";
        this.createdAt = Instant.now();
    }

    public ResumeSkill(Resume resume, Skill skill, BigDecimal extractionConfidence, String contextSnippet) {
        this.resume = resume;
        this.skill = skill;
        this.extractionConfidence = extractionConfidence != null ? extractionConfidence : new BigDecimal("1.000");
        this.contextSnippet = contextSnippet;
        this.extractionMethod = "EXACT_CANONICAL_MATCH";
        this.createdAt = Instant.now();
    }

    public ResumeSkill(Resume resume, Skill skill, BigDecimal extractionConfidence, String contextSnippet,
                       String matchedText, String extractionMethod) {
        this.resume = resume;
        this.skill = skill;
        this.extractionConfidence = extractionConfidence != null ? extractionConfidence : new BigDecimal("1.000");
        this.contextSnippet = contextSnippet;
        this.matchedText = matchedText;
        this.extractionMethod = extractionMethod != null ? extractionMethod : "EXACT_CANONICAL_MATCH";
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (extractionConfidence == null) extractionConfidence = new BigDecimal("1.000");
        if (extractionMethod == null) extractionMethod = "EXACT_CANONICAL_MATCH";
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Resume getResume() { return resume; }
    public void setResume(Resume resume) { this.resume = resume; }

    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }

    public BigDecimal getExtractionConfidence() { return extractionConfidence; }
    public void setExtractionConfidence(BigDecimal extractionConfidence) { this.extractionConfidence = extractionConfidence; }

    public String getContextSnippet() { return contextSnippet; }
    public void setContextSnippet(String contextSnippet) { this.contextSnippet = contextSnippet; }

    public String getMatchedText() { return matchedText; }
    public void setMatchedText(String matchedText) { this.matchedText = matchedText; }

    public String getExtractionMethod() { return extractionMethod; }
    public void setExtractionMethod(String extractionMethod) { this.extractionMethod = extractionMethod; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
