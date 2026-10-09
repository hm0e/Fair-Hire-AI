package com.fairhire.models;

import com.fairhire.models.enums.JobStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 100)
    private String department;

    @Column(length = 150)
    private String location;

    @Column(name = "employment_type", length = 50)
    private String employmentType;

    @Column(name = "remote_policy", length = 30)
    private String remotePolicy;

    @Column(name = "raw_description", nullable = false, columnDefinition = "TEXT")
    private String rawDescription;

    @Column(name = "rewritten_description", columnDefinition = "TEXT")
    private String rewrittenDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private JobStatus status = JobStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobRequirement> requirements = new ArrayList<>();

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobSkill> jobSkills = new ArrayList<>();

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BiasReport> biasReports = new ArrayList<>();

    public Job() {}

    public Job(String title, String rawDescription, User creator) {
        this.title = title;
        this.rawDescription = rawDescription;
        this.creator = creator;
        this.status = JobStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Job(String title, String rawDescription, String department, User creator) {
        this.title = title;
        this.rawDescription = rawDescription;
        this.department = department;
        this.creator = creator;
        this.status = JobStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
        if (status == null) status = JobStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public Long getCreatedBy() {
        return creator != null ? creator.getId() : null;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }

    public String getRemotePolicy() { return remotePolicy; }
    public void setRemotePolicy(String remotePolicy) { this.remotePolicy = remotePolicy; }

    public String getRawDescription() { return rawDescription; }
    public void setRawDescription(String rawDescription) { this.rawDescription = rawDescription; }

    // Convenience accessors for description
    public String getDescription() { return rawDescription; }
    public void setDescription(String description) { this.rawDescription = description; }

    public String getRewrittenDescription() { return rewrittenDescription; }
    public void setRewrittenDescription(String rewrittenDescription) { this.rewrittenDescription = rewrittenDescription; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<JobRequirement> getRequirements() { return requirements; }
    public void setRequirements(List<JobRequirement> requirements) { this.requirements = requirements; }

    public List<JobSkill> getJobSkills() { return jobSkills; }
    public void setJobSkills(List<JobSkill> jobSkills) { this.jobSkills = jobSkills; }

    public List<BiasReport> getBiasReports() { return biasReports; }
    public void setBiasReports(List<BiasReport> biasReports) { this.biasReports = biasReports; }
}
