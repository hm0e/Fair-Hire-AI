package com.fairhire.models;

import com.fairhire.models.enums.DatasetLifecycleStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dataset_versions")
public class DatasetVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version_tag", nullable = false, unique = true, length = 50)
    private String versionTag;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Column(name = "total_resumes_count", nullable = false)
    private Integer totalResumesCount = 0;

    @Column(name = "total_jds_count", nullable = false)
    private Integer totalJdsCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false, length = 30)
    private DatasetLifecycleStatus lifecycleStatus = DatasetLifecycleStatus.DRAFT;

    @Column(name = "frozen_at")
    private Instant frozenAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "datasetVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DatasetRecord> records = new ArrayList<>();

    public DatasetVersion() {}

    public DatasetVersion(String versionTag, String name, String description, String checksumSha256) {
        this.versionTag = versionTag;
        this.name = name;
        this.description = description;
        this.checksumSha256 = checksumSha256;
        this.lifecycleStatus = DatasetLifecycleStatus.DRAFT;
        this.totalResumesCount = 0;
        this.totalJdsCount = 0;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (lifecycleStatus == null) lifecycleStatus = DatasetLifecycleStatus.DRAFT;
        if (totalResumesCount == null) totalResumesCount = 0;
        if (totalJdsCount == null) totalJdsCount = 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVersionTag() { return versionTag; }
    public void setVersionTag(String versionTag) { this.versionTag = versionTag; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getChecksumSha256() { return checksumSha256; }
    public void setChecksumSha256(String checksumSha256) { this.checksumSha256 = checksumSha256; }

    public Integer getTotalResumesCount() { return totalResumesCount; }
    public void setTotalResumesCount(Integer totalResumesCount) { this.totalResumesCount = totalResumesCount; }

    public Integer getTotalJdsCount() { return totalJdsCount; }
    public void setTotalJdsCount(Integer totalJdsCount) { this.totalJdsCount = totalJdsCount; }

    public DatasetLifecycleStatus getLifecycleStatus() { return lifecycleStatus; }
    public void setLifecycleStatus(DatasetLifecycleStatus lifecycleStatus) { this.lifecycleStatus = lifecycleStatus; }

    public Instant getFrozenAt() { return frozenAt; }
    public void setFrozenAt(Instant frozenAt) { this.frozenAt = frozenAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public List<DatasetRecord> getRecords() { return records; }
    public void setRecords(List<DatasetRecord> records) { this.records = records; }
}
