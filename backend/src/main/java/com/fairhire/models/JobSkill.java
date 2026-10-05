package com.fairhire.models;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "job_skills", uniqueConstraints = {
    @UniqueConstraint(name = "uq_job_skills_job_skill", columnNames = {"job_id", "skill_id"})
})
public class JobSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(name = "is_mandatory", nullable = false)
    private Boolean isMandatory = true;

    @Column(name = "minimum_years", nullable = false)
    private Integer minimumYears = 0;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal weight = new BigDecimal("1.000");

    public JobSkill() {}

    public JobSkill(Job job, Skill skill) {
        this.job = job;
        this.skill = skill;
        this.isMandatory = true;
        this.minimumYears = 0;
        this.weight = new BigDecimal("1.000");
    }

    public JobSkill(Job job, Skill skill, Boolean isMandatory, Integer minimumYears, BigDecimal weight) {
        this.job = job;
        this.skill = skill;
        this.isMandatory = isMandatory != null ? isMandatory : true;
        this.minimumYears = minimumYears != null ? minimumYears : 0;
        this.weight = weight != null ? weight : new BigDecimal("1.000");
    }

    @PrePersist
    protected void onCreate() {
        if (isMandatory == null) isMandatory = true;
        if (minimumYears == null) minimumYears = 0;
        if (weight == null) weight = new BigDecimal("1.000");
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }

    public Boolean getIsMandatory() { return isMandatory; }
    public void setIsMandatory(Boolean isMandatory) { this.isMandatory = isMandatory; }

    public Integer getMinimumYears() { return minimumYears; }
    public void setMinimumYears(Integer minimumYears) { this.minimumYears = minimumYears; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
}
