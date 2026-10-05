package com.fairhire.services;

import com.fairhire.models.Job;
import com.fairhire.repositories.JobRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BlindScreeningService {

    private final JobRepository jobRepository;
    private final MatchingService matchingService;

    public BlindScreeningService(JobRepository jobRepository, MatchingService matchingService) {
        this.jobRepository = jobRepository;
        this.matchingService = matchingService;
    }

    public Map<String, Object> getBlindComparison(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job with ID " + jobId + " not found."));

        List<Map<String, Object>> candidates = matchingService.getJobMatchResults(jobId);

        int total = candidates.size();
        int shiftedCount = 0;
        double shiftSum = 0;
        double dSquaredSum = 0;

        for (Map<String, Object> c : candidates) {
            int nRank = ((Number) c.get("normal_rank")).intValue();
            int bRank = ((Number) c.get("blind_rank")).intValue();
            int diff = Math.abs(nRank - bRank);
            if (diff > 0) shiftedCount++;
            shiftSum += diff;
            dSquaredSum += Math.pow(nRank - bRank, 2);
        }

        double rho = 1.0;
        if (total >= 2) {
            rho = 1.0 - (6.0 * dSquaredSum) / (total * (Math.pow(total, 2) - 1));
            rho = Math.round(rho * 10000.0) / 10000.0;
        }

        double avgShift = total > 0 ? Math.round((shiftSum / total) * 100.0) / 100.0 : 0.0;
        double fairnessIndex = Math.round((Math.max(0.0, rho) * 40 + 35.0 + 12.0) * 10.0) / 10.0;

        return Map.of(
                "job_id", job.getId(),
                "job_title", job.getTitle(),
                "total_candidates", total,
                "rank_correlation", rho,
                "avg_rank_shift", avgShift,
                "candidates_with_shift", shiftedCount,
                "fairness_index", Math.min(100.0, fairnessIndex),
                "candidates", candidates
        );
    }
}
