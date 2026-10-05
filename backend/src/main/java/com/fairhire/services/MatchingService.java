package com.fairhire.services;

import com.fairhire.client.AIServiceClient;
import com.fairhire.models.*;
import com.fairhire.models.enums.MatchingMethod;
import com.fairhire.models.enums.ScreeningMode;
import com.fairhire.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class MatchingService {

    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final MatchResultRepository matchResultRepository;
    private final AIServiceClient aiServiceClient;

    public MatchingService(JobRepository jobRepository, ResumeRepository resumeRepository,
                           MatchResultRepository matchResultRepository, AIServiceClient aiServiceClient) {
        this.jobRepository = jobRepository;
        this.resumeRepository = resumeRepository;
        this.matchResultRepository = matchResultRepository;
        this.aiServiceClient = aiServiceClient;
    }

    @Transactional
    public List<Map<String, Object>> runJobMatching(Long jobId, Double keywordWeight, Double semanticWeight) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job with ID " + jobId + " not found."));

        List<Resume> resumes = resumeRepository.findAll();
        if (resumes.isEmpty()) {
            throw new IllegalStateException("No resumes found in the system to match.");
        }

        double kwW = keywordWeight != null ? keywordWeight : 0.3;
        double semW = semanticWeight != null ? semanticWeight : 0.7;
        double totalW = (kwW + semW) > 0 ? (kwW + semW) : 1.0;
        kwW /= totalW;
        semW /= totalW;
        final double finalKwW = kwW;
        final double finalSemW = semW;

        Set<String> jobSkills = new HashSet<>(job.getJobSkills().stream()
                .map(js -> js.getSkill().getName().toLowerCase())
                .toList());

        List<MatchScoreTemp> scoredList = new ArrayList<>();

        for (Resume resume : resumes) {
            Set<String> resumeSkills = new HashSet<>(resume.getResumeSkills().stream()
                    .map(rs -> rs.getSkill().getName().toLowerCase())
                    .toList());

            // 1. Keyword Score (Overlap)
            long overlap = resumeSkills.stream().filter(jobSkills::contains).count();
            double kwScore = !jobSkills.isEmpty() ? (double) overlap / jobSkills.size() : 0.5;

            // 2. Semantic Score via S-BERT embeddings
            Map<String, Object> semResult = aiServiceClient.computeSemanticScore(job.getDescription(), resume.getRawText());
            if (!(semResult.get("semantic_score") instanceof Number n)) {
                throw new IllegalStateException("AI semantic matching service unavailable: " + semResult.getOrDefault("error", "Service unavailable") + ". Synthetic scores are strictly disabled.");
            }
            double semScore = n.doubleValue();

            double finalScore = (kwW * kwScore) + (semW * semScore);
            double blindScore = finalScore;

            scoredList.add(new MatchScoreTemp(resume, kwScore, semScore, finalScore, blindScore));
        }

        // Rank by final_score descending
        scoredList.sort((a, b) -> Double.compare(b.finalScore, a.finalScore));
        for (int i = 0; i < scoredList.size(); i++) {
            scoredList.get(i).normalRank = i + 1;
        }

        // Rank by blind_score descending
        List<MatchScoreTemp> blindSorted = new ArrayList<>(scoredList);
        blindSorted.sort((a, b) -> Double.compare(b.blindScore, a.blindScore));
        for (int i = 0; i < blindSorted.size(); i++) {
            blindSorted.get(i).blindRank = i + 1;
        }

        // Save or update NORMAL match_results
        for (MatchScoreTemp temp : scoredList) {
            MatchResult normalResult = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                    job.getId(), temp.resume.getId(), ScreeningMode.NORMAL, MatchingMethod.HYBRID
            ).orElseGet(() -> new MatchResult(
                    job, temp.resume.getCandidate(), temp.resume, ScreeningMode.NORMAL, MatchingMethod.HYBRID,
                    BigDecimal.valueOf(temp.kwScore).setScale(4, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(temp.kwScore).setScale(4, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(temp.semScore).setScale(4, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(finalKwW).setScale(3, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(finalSemW).setScale(3, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(temp.finalScore).setScale(4, RoundingMode.HALF_UP),
                    temp.normalRank
            ));

            normalResult.setKeywordScore(BigDecimal.valueOf(temp.kwScore).setScale(4, RoundingMode.HALF_UP));
            normalResult.setSemanticScore(BigDecimal.valueOf(temp.semScore).setScale(4, RoundingMode.HALF_UP));
            normalResult.setFinalCompositeScore(BigDecimal.valueOf(temp.finalScore).setScale(4, RoundingMode.HALF_UP));
            normalResult.setRankInPool(temp.normalRank);
            matchResultRepository.save(normalResult);

            // Save or update BLIND match_results
            MatchResult blindResult = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                    job.getId(), temp.resume.getId(), ScreeningMode.BLIND, MatchingMethod.HYBRID
            ).orElseGet(() -> new MatchResult(
                    job, temp.resume.getCandidate(), temp.resume, ScreeningMode.BLIND, MatchingMethod.HYBRID,
                    BigDecimal.valueOf(temp.kwScore).setScale(4, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(temp.kwScore).setScale(4, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(temp.semScore).setScale(4, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(finalKwW).setScale(3, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(finalSemW).setScale(3, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(temp.blindScore).setScale(4, RoundingMode.HALF_UP),
                    temp.blindRank
            ));

            blindResult.setKeywordScore(BigDecimal.valueOf(temp.kwScore).setScale(4, RoundingMode.HALF_UP));
            blindResult.setSemanticScore(BigDecimal.valueOf(temp.semScore).setScale(4, RoundingMode.HALF_UP));
            blindResult.setFinalCompositeScore(BigDecimal.valueOf(temp.blindScore).setScale(4, RoundingMode.HALF_UP));
            blindResult.setRankInPool(temp.blindRank);
            matchResultRepository.save(blindResult);
        }

        return getJobMatchResults(jobId);
    }

    public List<Map<String, Object>> getJobMatchResults(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found."));

        List<MatchResult> normalMatches = matchResultRepository.findByJobIdAndScreeningModeOrderByRankInPoolAsc(jobId, ScreeningMode.NORMAL);
        if (normalMatches.isEmpty()) {
            return runJobMatching(jobId, 0.3, 0.7);
        }

        List<MatchResult> blindMatches = matchResultRepository.findByJobIdAndScreeningModeOrderByRankInPoolAsc(jobId, ScreeningMode.BLIND);
        Map<Long, Integer> blindRankByResumeId = new HashMap<>();
        for (MatchResult bm : blindMatches) {
            blindRankByResumeId.put(bm.getResume().getId(), bm.getRankInPool());
        }

        List<String> jobSkills = job.getJobSkills().stream()
                .map(js -> js.getSkill().getName())
                .toList();

        return normalMatches.stream().map(m -> {
            List<String> rSkills = m.getResume().getResumeSkills().stream()
                    .map(rs -> rs.getSkill().getName())
                    .toList();

            List<String> matched = jobSkills.stream().filter(rSkills::contains).toList();
            List<String> missing = jobSkills.stream().filter(s -> !rSkills.contains(s)).toList();

            int nRank = m.getRankInPool() != null ? m.getRankInPool() : 1;
            int bRank = blindRankByResumeId.getOrDefault(m.getResume().getId(), nRank);

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("job_id", m.getJob().getId());
            map.put("resume_id", m.getResume().getId());
            map.put("candidate_name", m.getCandidate().getFullName());
            map.put("candidate_email", m.getCandidate().getEmail() != null ? m.getCandidate().getEmail() : "");
            map.put("years_experience", m.getResume().getYearsExperience());
            map.put("keyword_score", Math.round(m.getKeywordScore().doubleValue() * 1000.0) / 1000.0);
            map.put("semantic_score", Math.round(m.getSemanticScore().doubleValue() * 1000.0) / 1000.0);
            map.put("final_score", Math.round(m.getFinalCompositeScore().doubleValue() * 1000.0) / 1000.0);
            map.put("normal_rank", nRank);
            map.put("blind_rank", bRank);
            map.put("rank_shift", Math.abs(nRank - bRank));
            map.put("matched_skills", matched);
            map.put("missing_skills", missing);
            return map;
        }).toList();
    }

    public Map<String, Object> getMatchDetail(Long id) {
        MatchResult m = matchResultRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Match result not found."));
        return Map.of(
                "id", m.getId(),
                "job_title", m.getJob().getTitle(),
                "candidate_name", m.getCandidate().getFullName(),
                "final_score", m.getFinalCompositeScore().doubleValue(),
                "normal_rank", m.getRankInPool(),
                "screening_mode", m.getScreeningMode().name()
        );
    }

    private static class MatchScoreTemp {
        Resume resume;
        double kwScore;
        double semScore;
        double finalScore;
        double blindScore;
        int normalRank;
        int blindRank;

        MatchScoreTemp(Resume resume, double kwScore, double semScore, double finalScore, double blindScore) {
            this.resume = resume;
            this.kwScore = kwScore;
            this.semScore = semScore;
            this.finalScore = finalScore;
            this.blindScore = blindScore;
        }
    }
}
