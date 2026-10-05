package com.fairhire.models;

import com.fairhire.models.enums.RewriteCategory;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "rewrite_suggestions")
public class RewriteSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bias_report_id", nullable = false)
    private BiasReport biasReport;

    @Column(name = "original_phrase", nullable = false, length = 100)
    private String originalPhrase;

    @Column(name = "suggested_replacement", nullable = false, length = 100)
    private String suggestedReplacement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RewriteCategory category;

    @Column(name = "context_snippet", nullable = false, length = 300)
    private String contextSnippet;

    @Column(name = "is_accepted", nullable = false)
    private Boolean isAccepted = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public RewriteSuggestion() {}

    public RewriteSuggestion(BiasReport biasReport, String originalPhrase, String suggestedReplacement,
                             RewriteCategory category, String contextSnippet, Boolean isAccepted) {
        this.biasReport = biasReport;
        this.originalPhrase = originalPhrase;
        this.suggestedReplacement = suggestedReplacement;
        this.category = category;
        this.contextSnippet = contextSnippet;
        this.isAccepted = isAccepted != null ? isAccepted : true;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (isAccepted == null) isAccepted = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BiasReport getBiasReport() { return biasReport; }
    public void setBiasReport(BiasReport biasReport) { this.biasReport = biasReport; }

    public String getOriginalPhrase() { return originalPhrase; }
    public void setOriginalPhrase(String originalPhrase) { this.originalPhrase = originalPhrase; }

    public String getSuggestedReplacement() { return suggestedReplacement; }
    public void setSuggestedReplacement(String suggestedReplacement) { this.suggestedReplacement = suggestedReplacement; }

    public RewriteCategory getCategory() { return category; }
    public void setCategory(RewriteCategory category) { this.category = category; }

    public String getContextSnippet() { return contextSnippet; }
    public void setContextSnippet(String contextSnippet) { this.contextSnippet = contextSnippet; }

    public Boolean getIsAccepted() { return isAccepted; }
    public void setIsAccepted(Boolean isAccepted) { this.isAccepted = isAccepted; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
