package com.fairhire.services;

import com.fairhire.client.AIServiceClient;
import com.fairhire.models.*;
import com.fairhire.models.enums.*;
import com.fairhire.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final BiasReportRepository biasReportRepository;
    private final AIServiceClient aiServiceClient;

    public JobService(JobRepository jobRepository, UserRepository userRepository,
                      SkillRepository skillRepository, BiasReportRepository biasReportRepository,
                      AIServiceClient aiServiceClient) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.biasReportRepository = biasReportRepository;
        this.aiServiceClient = aiServiceClient;
    }

    @Transactional
    public Job createJob(String title, String description, String requirements, Long createdBy) {
        User creator = null;
        if (createdBy != null) {
            creator = userRepository.findById(createdBy).orElse(null);
        }
        if (creator == null) {
            creator = userRepository.findAll().stream().findFirst().orElse(null);
        }
        if (creator == null) {
            creator = userRepository.save(new User("System Recruiter", "system.recruiter@fairhire.ai", "default-hash", UserRole.RECRUITER));
        }

        Job job = new Job(title.trim(), description.trim(), creator);
        if (requirements != null && !requirements.isBlank()) {
            job.getRequirements().add(new JobRequirement(
                    job, RequirementType.GENERAL, requirements.trim(), RequirementNecessity.REQUIRED, new BigDecimal("1.000")
            ));
        }
        job = jobRepository.save(job);

        String combinedText = (title + "\n" + description + "\n" + (requirements != null ? requirements : "")).toLowerCase();

        // 1. Skill associations
        List<Skill> allSkills = skillRepository.findAll();
        for (Skill sk : allSkills) {
            if (combinedText.contains(sk.getName().toLowerCase())) {
                job.getJobSkills().add(new JobSkill(job, sk));
            }
        }

        // 2. Bias analysis via AI Service Client
        try {
            Map<String, Object> biasAnalysis = aiServiceClient.analyzeBias(combinedText);
            if (biasAnalysis != null && !biasAnalysis.containsKey("error")) {
                BigDecimal genderScore = BigDecimal.valueOf(((Number) biasAnalysis.getOrDefault("gender_bias_score", 0.0)).doubleValue());
                BigDecimal ageScore = BigDecimal.valueOf(((Number) biasAnalysis.getOrDefault("age_bias_score", 0.0)).doubleValue());
                BigDecimal fleschScore = BigDecimal.valueOf(((Number) biasAnalysis.getOrDefault("flesch_reading_ease", 60.0)).doubleValue());

                BiasReport report = new BiasReport(
                        job, BiasTargetVersion.ORIGINAL, genderScore,
                        GenderLean.BALANCED, ageScore, fleschScore, ReadingLevel.MODERATE
                );

                Object flaggedWordsObj = biasAnalysis.get("flagged_words");
                if (flaggedWordsObj instanceof List<?> flaggedList) {
                    for (Object item : flaggedList) {
                        if (item instanceof Map<?, ?> map) {
                            String word = String.valueOf(map.get("word"));
                            String cat = String.valueOf(map.get("category"));
                            Object suggObj = map.get("suggestions");
                            String sugg = suggObj != null ? String.valueOf(suggObj) : "Consider neutral alternative";
                            RewriteCategory rwCat = cat.toUpperCase().contains("GENDER") ? RewriteCategory.GENDER_MASCULINE : RewriteCategory.AGE_EXCLUSIONARY;
                            report.getRewriteSuggestions().add(new RewriteSuggestion(
                                    report, word, sugg, rwCat, word, true
                            ));
                        }
                    }
                    report.setTotalFlaggedTerms(report.getRewriteSuggestions().size());
                }
                job.getBiasReports().add(report);
            }
        } catch (Exception ignored) {
            // Safe handling if AI service is not running during job creation
        }

        return jobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllJobs() {
        return jobRepository.findAllByOrderByIdDesc().stream()
                .map(this::formatJobDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getJobById(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job with ID " + id + " not found."));
        return formatJobDto(job);
    }

    @Transactional
    public Map<String, Object> updateJob(Long id, String title, String description, String requirements) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found."));

        if (title != null) job.setTitle(title.trim());
        if (description != null) job.setDescription(description.trim());
        if (requirements != null && !requirements.isBlank()) {
            job.getRequirements().clear();
            job.getRequirements().add(new JobRequirement(
                    job, RequirementType.GENERAL, requirements.trim(), RequirementNecessity.REQUIRED, new BigDecimal("1.000")
            ));
        }

        return formatJobDto(jobRepository.save(job));
    }

    @Transactional
    public void deleteJob(Long id) {
        if (!jobRepository.existsById(id)) {
            throw new IllegalArgumentException("Job with ID " + id + " does not exist.");
        }
        jobRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> formatJobDto(Job job) {
        List<String> skills = (job.getJobSkills() != null)
                ? job.getJobSkills().stream()
                    .map(js -> js.getSkill() != null ? js.getSkill().getName() : "")
                    .filter(s -> !s.isBlank())
                    .toList()
                : List.of();

        List<Map<String, Object>> biasItems = (job.getBiasReports() != null)
                ? job.getBiasReports().stream()
                    .flatMap(br -> br.getRewriteSuggestions().stream().map(rs -> Map.<String, Object>of(
                            "id", rs.getId(),
                            "phrase", rs.getOriginalPhrase(),
                            "category", rs.getCategory().name(),
                            "suggestion", rs.getSuggestedReplacement()
                    )))
                    .toList()
                : List.of();

        String reqStr = job.getRequirements().isEmpty() ? "" :
                job.getRequirements().get(0).getRequirementValue();

        return Map.of(
                "id", job.getId(),
                "title", job.getTitle(),
                "description", job.getDescription(),
                "requirements", reqStr,
                "skills", skills,
                "bias_reports", biasItems,
                "bias_count", biasItems.size(),
                "created_at", job.getCreatedAt().toString()
        );
    }
}
