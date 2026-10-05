package com.fairhire.models;

import com.fairhire.models.enums.ParsingStatus;
import com.fairhire.models.enums.ResumeFileType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 20)
    private ResumeFileType fileType = ResumeFileType.PDF;

    @Column(name = "file_hash_sha256", nullable = false, length = 64)
    private String fileHashSha256;

    @Column(name = "raw_text", nullable = false, columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "parsed_years_experience", nullable = false, precision = 4, scale = 1)
    private BigDecimal parsedYearsExperience = BigDecimal.ZERO;

    @Column(name = "parsed_education", columnDefinition = "TEXT")
    private String parsedEducation;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "parser_version", nullable = false, length = 50)
    private String parserVersion = "v1.0-deterministic";

    @Column(name = "parsing_error", columnDefinition = "TEXT")
    private String parsingError;

    @Enumerated(EnumType.STRING)
    @Column(name = "parsing_status", nullable = false, length = 30)
    private ParsingStatus parsingStatus = ParsingStatus.UPLOADED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "resume")
    private List<ResumeSkill> resumeSkills = new ArrayList<>();

    @OneToOne(mappedBy = "resume", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private BlindScreeningResult blindScreeningResult;

    public Resume() {}

    public Resume(Candidate candidate, String fileName, ResumeFileType fileType, String fileHashSha256,
                  String rawText, BigDecimal parsedYearsExperience, String parsedEducation) {
        this.candidate = candidate;
        this.fileName = fileName;
        this.fileType = fileType != null ? fileType : ResumeFileType.PDF;
        this.fileHashSha256 = fileHashSha256;
        this.rawText = rawText;
        this.parsedYearsExperience = parsedYearsExperience != null ? parsedYearsExperience : BigDecimal.ZERO;
        this.parsedEducation = parsedEducation;
        this.parsingStatus = ParsingStatus.COMPLETED;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
        if (parsedYearsExperience == null) parsedYearsExperience = BigDecimal.ZERO;
        if (parsingStatus == null) parsingStatus = ParsingStatus.UPLOADED;
        if (fileType == null) fileType = ResumeFileType.PDF;
        if (rawText == null) rawText = "";
        if (parserVersion == null) parserVersion = "v1.0-deterministic";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Candidate getCandidate() { return candidate; }
    public void setCandidate(Candidate candidate) { this.candidate = candidate; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public ResumeFileType getFileType() { return fileType; }
    public void setFileType(ResumeFileType fileType) { this.fileType = fileType; }

    public String getFileHashSha256() { return fileHashSha256; }
    public void setFileHashSha256(String fileHashSha256) { this.fileHashSha256 = fileHashSha256; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getParserVersion() { return parserVersion; }
    public void setParserVersion(String parserVersion) { this.parserVersion = parserVersion; }

    public String getParsingError() { return parsingError; }
    public void setParsingError(String parsingError) { this.parsingError = parsingError; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public BigDecimal getParsedYearsExperience() { return parsedYearsExperience; }
    public void setParsedYearsExperience(BigDecimal parsedYearsExperience) { this.parsedYearsExperience = parsedYearsExperience; }

    // Convenience accessors for yearsExperience
    public Double getYearsExperience() {
        return parsedYearsExperience != null ? parsedYearsExperience.doubleValue() : 0.0;
    }
    public void setYearsExperience(Double yearsExperience) {
        this.parsedYearsExperience = yearsExperience != null ? BigDecimal.valueOf(yearsExperience) : BigDecimal.ZERO;
    }

    public String getParsedEducation() { return parsedEducation; }
    public void setParsedEducation(String parsedEducation) { this.parsedEducation = parsedEducation; }

    public String getEducation() { return parsedEducation; }
    public void setEducation(String education) { this.parsedEducation = education; }

    public ParsingStatus getParsingStatus() { return parsingStatus; }
    public void setParsingStatus(ParsingStatus parsingStatus) { this.parsingStatus = parsingStatus; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<ResumeSkill> getResumeSkills() { return resumeSkills; }
    public void setResumeSkills(List<ResumeSkill> resumeSkills) { this.resumeSkills = resumeSkills; }

    public BlindScreeningResult getBlindScreeningResult() { return blindScreeningResult; }
    public void setBlindScreeningResult(BlindScreeningResult blindScreeningResult) { this.blindScreeningResult = blindScreeningResult; }

    public String getAnonymizedText() {
        return blindScreeningResult != null ? blindScreeningResult.getAnonymizedText() : null;
    }
}
