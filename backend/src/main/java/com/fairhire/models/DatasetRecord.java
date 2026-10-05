package com.fairhire.models;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dataset_records", uniqueConstraints = {
    @UniqueConstraint(name = "uq_dataset_records_version_identifier",
                      columnNames = {"dataset_version_id", "record_identifier"})
})
public class DatasetRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_version_id", nullable = false)
    private DatasetVersion datasetVersion;

    @Column(name = "record_identifier", nullable = false, length = 100)
    private String recordIdentifier;

    @Column(name = "jd_title", nullable = false, length = 200)
    private String jdTitle;

    @Column(name = "jd_text", nullable = false, columnDefinition = "TEXT")
    private String jdText;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "jd_required_skills")
    private List<String> jdRequiredSkills = new ArrayList<>();

    @Column(name = "candidate_identifier", nullable = false, length = 100)
    private String candidateIdentifier;

    @Column(name = "candidate_raw_resume", nullable = false, columnDefinition = "TEXT")
    private String candidateRawResume;

    @Column(name = "candidate_anonymized_resume", nullable = false, columnDefinition = "TEXT")
    private String candidateAnonymizedResume;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "candidate_skills")
    private List<String> candidateSkills = new ArrayList<>();

    /**
     * Strictly authorized, explicitly sourced research labels only.
     * Never inferred from candidate data or used in scoring algorithms.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "demographic_ground_truth")
    private String demographicGroundTruth;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "datasetRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Annotation> annotations = new ArrayList<>();

    public DatasetRecord() {}

    public DatasetRecord(DatasetVersion datasetVersion, String recordIdentifier, String jdTitle,
                         String jdText, List<String> jdRequiredSkills, String candidateIdentifier,
                         String candidateRawResume, String candidateAnonymizedResume,
                         List<String> candidateSkills, String demographicGroundTruth) {
        this.datasetVersion = datasetVersion;
        this.recordIdentifier = recordIdentifier;
        this.jdTitle = jdTitle;
        this.jdText = jdText;
        this.jdRequiredSkills = jdRequiredSkills != null ? jdRequiredSkills : new ArrayList<>();
        this.candidateIdentifier = candidateIdentifier;
        this.candidateRawResume = candidateRawResume;
        this.candidateAnonymizedResume = candidateAnonymizedResume;
        this.candidateSkills = candidateSkills != null ? candidateSkills : new ArrayList<>();
        this.demographicGroundTruth = demographicGroundTruth;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (jdRequiredSkills == null) jdRequiredSkills = new ArrayList<>();
        if (candidateSkills == null) candidateSkills = new ArrayList<>();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DatasetVersion getDatasetVersion() { return datasetVersion; }
    public void setDatasetVersion(DatasetVersion datasetVersion) { this.datasetVersion = datasetVersion; }

    public String getRecordIdentifier() { return recordIdentifier; }
    public void setRecordIdentifier(String recordIdentifier) { this.recordIdentifier = recordIdentifier; }

    public String getJdTitle() { return jdTitle; }
    public void setJdTitle(String jdTitle) { this.jdTitle = jdTitle; }

    public String getJdText() { return jdText; }
    public void setJdText(String jdText) { this.jdText = jdText; }

    public List<String> getJdRequiredSkills() { return jdRequiredSkills; }
    public void setJdRequiredSkills(List<String> jdRequiredSkills) { this.jdRequiredSkills = jdRequiredSkills; }

    public String getCandidateIdentifier() { return candidateIdentifier; }
    public void setCandidateIdentifier(String candidateIdentifier) { this.candidateIdentifier = candidateIdentifier; }

    public String getCandidateRawResume() { return candidateRawResume; }
    public void setCandidateRawResume(String candidateRawResume) { this.candidateRawResume = candidateRawResume; }

    public String getCandidateAnonymizedResume() { return candidateAnonymizedResume; }
    public void setCandidateAnonymizedResume(String candidateAnonymizedResume) { this.candidateAnonymizedResume = candidateAnonymizedResume; }

    public List<String> getCandidateSkills() { return candidateSkills; }
    public void setCandidateSkills(List<String> candidateSkills) { this.candidateSkills = candidateSkills; }

    public String getDemographicGroundTruth() { return demographicGroundTruth; }
    public void setDemographicGroundTruth(String demographicGroundTruth) { this.demographicGroundTruth = demographicGroundTruth; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public List<Annotation> getAnnotations() { return annotations; }
    public void setAnnotations(List<Annotation> annotations) { this.annotations = annotations; }
}
