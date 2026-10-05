package com.fairhire.services;

import com.fairhire.client.AIServiceClient;
import com.fairhire.models.Job;
import com.fairhire.repositories.JobRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BiasService {

    private final JobRepository jobRepository;
    private final AIServiceClient aiServiceClient;

    public BiasService(JobRepository jobRepository, AIServiceClient aiServiceClient) {
        this.jobRepository = jobRepository;
        this.aiServiceClient = aiServiceClient;
    }

    public Map<String, Object> analyzeText(String text) {
        return aiServiceClient.analyzeBias(text);
    }

    public Map<String, Object> getJobBiasReport(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job with ID " + jobId + " not found."));

        Map<String, Object> liveAnalysis = aiServiceClient.analyzeBias(job.getDescription());

        List<Map<String, Object>> reports = (job.getBiasReports() != null)
                ? job.getBiasReports().stream()
                    .flatMap(br -> br.getRewriteSuggestions().stream().map(rs -> Map.<String, Object>of(
                            "id", rs.getId(),
                            "phrase", rs.getOriginalPhrase(),
                            "category", rs.getCategory().name(),
                            "severity", "medium",
                            "suggestion", rs.getSuggestedReplacement() != null ? rs.getSuggestedReplacement() : ""
                    )))
                    .toList()
                : List.of();

        return Map.of(
                "job_id", job.getId(),
                "job_title", job.getTitle(),
                "bias_score", liveAnalysis.getOrDefault("bias_score", 0.0),
                "bias_level", liveAnalysis.getOrDefault("bias_level", "low"),
                "readability", liveAnalysis.getOrDefault("readability", Map.of("flesch_score", 60)),
                "reports", reports,
                "flagged_words", liveAnalysis.getOrDefault("flagged_words", List.of())
        );
    }

    public Map<String, Object> rewriteText(String text) {
        return aiServiceClient.rewriteInclusive(text);
    }
}
