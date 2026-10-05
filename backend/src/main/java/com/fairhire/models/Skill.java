package com.fairhire.models;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "skills")
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String category;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "synonyms")
    private List<String> synonyms = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Skill() {}

    public Skill(String name, String category) {
        this.name = name;
        this.category = category;
        this.synonyms = new ArrayList<>();
        this.isActive = true;
        this.createdAt = Instant.now();
    }

    public Skill(String name, String category, List<String> synonyms) {
        this.name = name;
        this.category = category;
        this.synonyms = synonyms != null ? synonyms : new ArrayList<>();
        this.isActive = true;
        this.createdAt = Instant.now();
    }

    public Skill(String name, String category, List<String> synonyms, Boolean isActive) {
        this.name = name;
        this.category = category;
        this.synonyms = synonyms != null ? synonyms : new ArrayList<>();
        this.isActive = isActive != null ? isActive : true;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (synonyms == null) synonyms = new ArrayList<>();
        if (isActive == null) isActive = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // Convenience accessors for skillName
    public String getSkillName() { return name; }
    public void setSkillName(String skillName) { this.name = skillName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getSynonyms() { return synonyms; }
    public void setSynonyms(List<String> synonyms) { this.synonyms = synonyms; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
