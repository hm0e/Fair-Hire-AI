package com.fairhire.models;

import com.fairhire.models.enums.RequirementNecessity;
import com.fairhire.models.enums.RequirementType;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "job_requirements")
public class JobRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(name = "requirement_type", nullable = false, length = 50)
    private RequirementType requirementType;

    @Column(name = "requirement_value", nullable = false, length = 255)
    private String requirementValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequirementNecessity necessity = RequirementNecessity.REQUIRED;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal weight = new BigDecimal("1.000");

    public JobRequirement() {}

    public JobRequirement(Job job, RequirementType requirementType, String requirementValue, RequirementNecessity necessity, BigDecimal weight) {
        this.job = job;
        this.requirementType = requirementType;
        this.requirementValue = requirementValue;
        this.necessity = necessity != null ? necessity : RequirementNecessity.REQUIRED;
        this.weight = weight != null ? weight : new BigDecimal("1.000");
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public RequirementType getRequirementType() { return requirementType; }
    public void setRequirementType(RequirementType requirementType) { this.requirementType = requirementType; }

    public String getRequirementValue() { return requirementValue; }
    public void setRequirementValue(String requirementValue) { this.requirementValue = requirementValue; }

    public RequirementNecessity getNecessity() { return necessity; }
    public void setNecessity(RequirementNecessity necessity) { this.necessity = necessity; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
}
