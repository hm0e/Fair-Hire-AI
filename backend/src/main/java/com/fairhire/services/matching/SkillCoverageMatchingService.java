package com.fairhire.services.matching;

import com.fairhire.dto.*;
import com.fairhire.models.*;
import com.fairhire.models.enums.*;
import com.fairhire.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service orchestrating deterministic canonical skill matching and scoring.
 * Coordinates pure scoring calculation, PII scrubbing, atomic detail persistence, and staleness handling.
 */
@Service
public class SkillCoverageMatchingService {

    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final MatchResultRepository matchResultRepository;
    private final MatchSkillDetailRepository matchSkillDetailRepository;
    private final DeterministicScoringEngine scoringEngine;
    private final ResourceAuthorizationService authorizationService;
    private final TransactionTemplate transactionTemplate;

    private final ConcurrentHashMap<Long, Object> jobLocks = new ConcurrentHashMap<>();

    public SkillCoverageMatchingService(
            JobRepository jobRepository,
            ResumeRepository resumeRepository,
            MatchResultRepository matchResultRepository,
            MatchSkillDetailRepository matchSkillDetailRepository,
            DeterministicScoringEngine scoringEngine,
            ResourceAuthorizationService authorizationService,
            PlatformTransactionManager transactionManager
    ) {
        this.jobRepository = jobRepository;
        this.resumeRepository = resumeRepository;
        this.matchResultRepository = matchResultRepository;
        this.matchSkillDetailRepository = matchSkillDetailRepository;
        this.scoringEngine = scoringEngine;
        this.authorizationService = authorizationService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    private Object getJobLock(Long jobId) {
        return jobLocks.computeIfAbsent(jobId, id -> new Object());
    }

    /**
     * Match a job against a set of candidate resumes deterministically.
     * Serialized per jobId to prevent race conditions and duplicate MatchResult/MatchSkillDetail persistence.
     *
     * @param jobId Target job ID
     * @param requestedResumeIds Optional list of explicit resume IDs (if null or empty, matches all ready resumes)
     * @param callerDepartment Caller department (optional for authorization)
     * @param resumeDepartments Optional map of resume IDs to departments
     * @return MatchBatchResponseDto containing evaluation results
     */
    public MatchBatchResponseDto matchJob(Long jobId, List<Long> requestedResumeIds,
                                          String callerDepartment, Map<Long, String> resumeDepartments,
                                          String screeningModeStr, String matchingMethodStr) {
        if (jobId == null) {
            throw new ResourceNotFoundException("Job ID cannot be null.");
        }
        synchronized (getJobLock(jobId)) {
            return transactionTemplate.execute(status ->
                    executeMatchJob(jobId, requestedResumeIds, callerDepartment, resumeDepartments, screeningModeStr, matchingMethodStr)
            );
        }
    }

    private MatchBatchResponseDto executeMatchJob(Long jobId, List<Long> requestedResumeIds,
                                                  String callerDepartment, Map<Long, String> resumeDepartments,
                                                  String screeningModeStr, String matchingMethodStr) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job with ID " + jobId + " not found."));

        // 1. Authorize caller access to job (strict fail-closed boundary)
        authorizationService.validateJobAccess(job, callerDepartment);

        // 2. Validate request parameters
        final ScreeningMode screeningMode = parseScreeningMode(screeningModeStr);

        if (matchingMethodStr != null && !matchingMethodStr.isBlank()) {
            if (!MatchingMethod.CANONICAL_SKILL_COVERAGE.name().equalsIgnoreCase(matchingMethodStr.trim())) {
                throw new InvalidMatchingMethodException(
                        "Matching method '" + matchingMethodStr + "' is not supported for new matching execution. Only CANONICAL_SKILL_COVERAGE is permitted for Phase 4 deterministic matching."
                );
            }
        }

        // 3. Extract and partition canonical skill requirements
        List<JobSkill> jobSkills = job.getJobSkills() != null ? new ArrayList<>(job.getJobSkills()) : Collections.emptyList();
        // Deterministic sorting of job skills by ID
        jobSkills.sort(Comparator.comparing(JobSkill::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        Set<Long> requiredSkillIds = new HashSet<>();
        Set<Long> rawPreferredSkillIds = new HashSet<>();

        for (JobSkill js : jobSkills) {
            if (js.getSkill() != null && js.getSkill().getId() != null) {
                if (Boolean.TRUE.equals(js.getIsMandatory())) {
                    requiredSkillIds.add(js.getSkill().getId());
                } else {
                    rawPreferredSkillIds.add(js.getSkill().getId());
                }
            }
        }

        // Overlap resolution: REQUIRED takes precedence over PREFERRED
        Set<Long> preferredSkillIds = new HashSet<>(rawPreferredSkillIds);
        preferredSkillIds.removeAll(requiredSkillIds);

        // A job with zero recognized required skills MUST NOT enter normal matching
        if (requiredSkillIds.isEmpty()) {
            throw new MatchingRequirementsNotFoundException(
                    "Job " + jobId + " has zero recognized canonical skill requirements. Matching is unavailable."
            );
        }

        // 3. Resolve candidate resumes
        List<Resume> targetResumes = new ArrayList<>();
        if (requestedResumeIds != null && !requestedResumeIds.isEmpty()) {
            if (requestedResumeIds.size() > 100) {
                throw new BatchSizeLimitExceededException(
                        "Requested candidate batch size (" + requestedResumeIds.size() +
                        ") exceeds maximum synchronous limit (100). Filter candidates by department, status, or explicit resume IDs."
                );
            }
            for (Long resumeId : requestedResumeIds) {
                Resume r = resumeRepository.findById(resumeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Resume with ID " + resumeId + " not found."));
                targetResumes.add(r);
            }
        } else {
            List<Resume> allResumes = resumeRepository.findAll();
            targetResumes = allResumes.stream()
                    .filter(r -> r.getParsingStatus() == ParsingStatus.READY || r.getParsingStatus() == ParsingStatus.COMPLETED)
                    .collect(Collectors.toList());

            if (targetResumes.size() > 100) {
                throw new BatchSizeLimitExceededException(
                        "Requested candidate batch size (" + targetResumes.size() +
                        ") exceeds maximum synchronous limit (100). Filter candidates by department, status, or explicit resume IDs."
                );
            }
        }

        // 4. Evaluate each resume deterministically
        List<MatchResult> savedResults = new ArrayList<>();
        for (Resume resume : targetResumes) {
            // Authorize resume access
            authorizationService.validateResumeAccess(job, resume, resumeDepartments);

            // Extract candidate skills
            Map<Long, ResumeSkill> candidateSkillMap = new HashMap<>();
            if (resume.getResumeSkills() != null) {
                for (ResumeSkill rs : resume.getResumeSkills()) {
                    if (rs.getSkill() != null && rs.getSkill().getId() != null) {
                        candidateSkillMap.put(rs.getSkill().getId(), rs);
                    }
                }
            }
            Set<Long> candidateSkillIds = candidateSkillMap.keySet();

            // Execute pure mathematical scoring (zero PII passed)
            DeterministicScoringEngine.MatchScoreResult score = scoringEngine.computeScore(
                    candidateSkillIds, requiredSkillIds, preferredSkillIds
            );

            // Upsert deterministic MatchResult
            MatchResult matchResult = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                    job.getId(), resume.getId(), screeningMode, MatchingMethod.CANONICAL_SKILL_COVERAGE
            ).orElseGet(() -> {
                MatchResult nr = new MatchResult();
                nr.setJob(job);
                nr.setCandidate(resume.getCandidate());
                nr.setResume(resume);
                nr.setScreeningMode(screeningMode);
                nr.setMatchingMethod(MatchingMethod.CANONICAL_SKILL_COVERAGE);
                return nr;
            });

            // If existing result, clear previous details to ensure atomic replacement via orphanRemoval
            if (matchResult.getSkillDetails() != null) {
                matchResult.getSkillDetails().clear();
            }

            matchResult.setRequiredSkillCoverage(score.requiredSkillCoverage());
            matchResult.setPreferredSkillCoverage(score.preferredSkillCoverage());
            matchResult.setOverallScore(score.overallScore());
            matchResult.setRequiredSkillsTotal(score.requiredSkillsTotal());
            matchResult.setRequiredSkillsMatched(score.requiredSkillsMatched());
            matchResult.setPreferredSkillsTotal(score.preferredSkillsTotal());
            matchResult.setPreferredSkillsMatched(score.preferredSkillsMatched());
            matchResult.setIsStale(false);
            matchResult.setAlgorithmVersion("deterministic-v1");
            matchResult.setScoredAt(Instant.now());
            matchResult.setFinalCompositeScore(score.overallScore().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
            matchResult.setMatchedSkillsCount(score.requiredSkillsMatched() + score.preferredSkillsMatched());
            matchResult.setMissingSkillsCount(
                    (score.requiredSkillsTotal() - score.requiredSkillsMatched()) +
                    (score.preferredSkillsTotal() - score.preferredSkillsMatched())
            );

            MatchResult savedMatch = matchResultRepository.saveAndFlush(matchResult);

            // Generate MatchSkillDetail records
            List<MatchSkillDetail> details = new ArrayList<>();
            for (JobSkill js : jobSkills) {
                if (js.getSkill() == null || js.getSkill().getId() == null) continue;

                Long skillId = js.getSkill().getId();
                RequirementNecessity necessity = requiredSkillIds.contains(skillId)
                        ? RequirementNecessity.REQUIRED
                        : RequirementNecessity.PREFERRED;

                ResumeSkill rs = candidateSkillMap.get(skillId);
                boolean isMatched = (rs != null);
                BigDecimal candConfidence = rs != null ? rs.getExtractionConfidence() : null;
                String candMatchedText = rs != null ? DeterministicPiiScrubber.scrubMatchedText(rs.getMatchedText()) : null;
                String candContextSnippet = rs != null ? DeterministicPiiScrubber.scrubContextSnippet(rs.getContextSnippet()) : null;

                MatchSkillDetail detail = new MatchSkillDetail(
                        savedMatch, js, js.getSkill(), necessity, isMatched,
                        candConfidence, candMatchedText, candContextSnippet
                );
                details.add(detail);
                if (savedMatch.getSkillDetails() != null) {
                    savedMatch.getSkillDetails().add(detail);
                }
            }

            matchResultRepository.saveAndFlush(savedMatch);
            savedResults.add(savedMatch);
        }

        // 5. Deterministic ranking across pool: overallScore DESC, tie-break by resume ID ASC
        savedResults.sort((a, b) -> {
            int cmp = b.getOverallScore().compareTo(a.getOverallScore());
            if (cmp != 0) return cmp;
            return a.getResume().getId().compareTo(b.getResume().getId());
        });

        List<MatchResponseDto> responseList = new ArrayList<>();
        for (int i = 0; i < savedResults.size(); i++) {
            MatchResult mr = savedResults.get(i);
            mr.setRankInPool(i + 1);
            matchResultRepository.save(mr);
            responseList.add(mapToResponseDto(mr));
        }

        return new MatchBatchResponseDto(
                job.getId(),
                "deterministic-v1",
                MatchingMethod.CANONICAL_SKILL_COVERAGE.name(),
                savedResults.size(),
                responseList
        );
    }

    /**
     * Overload for matchJob with department context and default screeningMode/matchingMethod.
     */
    @Transactional
    public MatchBatchResponseDto matchJob(Long jobId, List<Long> requestedResumeIds,
                                          String callerDepartment, Map<Long, String> resumeDepartments) {
        return matchJob(jobId, requestedResumeIds, callerDepartment, resumeDepartments, null, null);
    }

    /**
     * Programmatic overload for internal tests, passing the job's department as caller context.
     */
    @Transactional
    public MatchBatchResponseDto matchJob(Long jobId, List<Long> requestedResumeIds) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job with ID " + jobId + " not found."));
        return matchJob(jobId, requestedResumeIds, job.getDepartment(), null, null, null);
    }

    /**
     * Single candidate resume match execution with explicit caller department.
     */
    @Transactional
    public MatchResponseDto matchSingleCandidate(Long jobId, Long resumeId, String callerDepartment) {
        MatchBatchResponseDto batch = matchJob(jobId, List.of(resumeId), callerDepartment, null);
        if (batch.getResults() != null && !batch.getResults().isEmpty()) {
            return batch.getResults().get(0);
        }
        throw new ResourceNotFoundException("No match produced for job " + jobId + " and resume " + resumeId);
    }

    /**
     * Single candidate resume match execution for internal tests (derives job department for test setup).
     */
    @Transactional
    public MatchResponseDto matchSingleCandidate(Long jobId, Long resumeId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job with ID " + jobId + " not found."));
        return matchSingleCandidate(jobId, resumeId, job.getDepartment());
    }

    /**
     * Get detailed decomposed match result including MatchSkillDetails with explainability metadata.
     * Read-only and authoritative: reads persisted MatchResult and MatchSkillDetail records.
     */
    @Transactional(readOnly = true)
    public MatchDetailResponseDto getMatchDetail(Long jobId, Long resumeId, String callerDepartment) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job with ID " + jobId + " not found."));

        // Enforce resource authorization for caller
        authorizationService.validateJobAccess(job, callerDepartment);

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume with ID " + resumeId + " not found."));

        // Enforce resume access authorization
        authorizationService.validateResumeAccess(job, resume, null);

        // First attempt to find deterministic CANONICAL_SKILL_COVERAGE match result
        Optional<MatchResult> deterministicMatch = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                jobId, resumeId, ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        );

        MatchResult mr;
        if (deterministicMatch.isPresent()) {
            mr = deterministicMatch.get();
        } else {
            // Check for legacy SEMANTIC match result
            Optional<MatchResult> semanticMatch = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                    jobId, resumeId, ScreeningMode.NORMAL, MatchingMethod.SEMANTIC
            );
            if (semanticMatch.isPresent()) {
                mr = semanticMatch.get();
            } else {
                List<MatchResult> anyMatches = matchResultRepository.findByJobIdAndResumeId(jobId, resumeId);
                if (!anyMatches.isEmpty()) {
                    mr = anyMatches.get(0);
                } else {
                    throw new ResourceNotFoundException(
                            "Match result not found for job " + jobId + " and resume " + resumeId
                    );
                }
            }
        }

        MatchDetailResponseDto dto = new MatchDetailResponseDto();
        dto.setMatchId(mr.getId());
        dto.setJobId(mr.getJob().getId());
        dto.setResumeId(mr.getResume().getId());
        dto.setCandidateId(mr.getCandidate() != null ? mr.getCandidate().getId() : null);
        dto.setOverallScore(mr.getOverallScore());
        dto.setIsStale(mr.getIsStale());
        dto.setStatus(Boolean.TRUE.equals(mr.getIsStale()) ? "STALE" : "ACTIVE");
        dto.setMatchingMethod(mr.getMatchingMethod() != null ? mr.getMatchingMethod().name() : null);
        dto.setScoredAt(mr.getScoredAt() != null ? mr.getScoredAt() : mr.getCreatedAt());

        // Handle legacy SEMANTIC match result without fabricating canonical skill details
        if (mr.getMatchingMethod() == MatchingMethod.SEMANTIC) {
            dto.setAlgorithmVersion(mr.getModelVersion() != null ? mr.getModelVersion() : "SEMANTIC");
            if (mr.getOverallScore() == null && mr.getSemanticScore() != null) {
                dto.setOverallScore(mr.getSemanticScore().multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP));
            }
            dto.setRequiredSkills(Collections.emptyList());
            dto.setPreferredSkills(Collections.emptyList());
            return dto;
        }

        // Canonical deterministic match result
        dto.setAlgorithmVersion(mr.getAlgorithmVersion() != null ? mr.getAlgorithmVersion() : "deterministic-v1");
        dto.setRequiredSkillCoverage(mr.getRequiredSkillCoverage());
        dto.setPreferredSkillCoverage(mr.getPreferredSkillCoverage());
        dto.setRequiredSkillsMatched(mr.getRequiredSkillsMatched());
        dto.setRequiredSkillsTotal(mr.getRequiredSkillsTotal());
        dto.setPreferredSkillsMatched(mr.getPreferredSkillsMatched());
        dto.setPreferredSkillsTotal(mr.getPreferredSkillsTotal());

        // Weights and contributions calculation adhering strictly to Phase 4B scoring semantics
        int reqTotal = mr.getRequiredSkillsTotal() != null ? mr.getRequiredSkillsTotal() : 0;
        int prefTotal = mr.getPreferredSkillsTotal() != null ? mr.getPreferredSkillsTotal() : 0;

        BigDecimal reqWeight;
        BigDecimal prefWeight;
        BigDecimal reqContribution;
        BigDecimal prefContribution;

        if (reqTotal > 0 && prefTotal > 0) {
            reqWeight = new BigDecimal("0.80");
            prefWeight = new BigDecimal("0.20");
            reqContribution = mr.getRequiredSkillCoverage() != null
                    ? mr.getRequiredSkillCoverage().multiply(new BigDecimal("0.80")).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            prefContribution = mr.getPreferredSkillCoverage() != null
                    ? mr.getPreferredSkillCoverage().multiply(new BigDecimal("0.20")).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else if (reqTotal > 0) {
            reqWeight = new BigDecimal("1.00");
            prefWeight = new BigDecimal("0.00");
            reqContribution = mr.getRequiredSkillCoverage() != null
                    ? mr.getRequiredSkillCoverage().multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            prefContribution = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else if (prefTotal > 0) {
            reqWeight = new BigDecimal("0.00");
            prefWeight = new BigDecimal("1.00");
            reqContribution = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            prefContribution = mr.getPreferredSkillCoverage() != null
                    ? mr.getPreferredSkillCoverage().multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            reqWeight = new BigDecimal("0.80");
            prefWeight = new BigDecimal("0.20");
            reqContribution = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            prefContribution = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        dto.setRequiredWeight(reqWeight);
        dto.setPreferredWeight(prefWeight);
        dto.setRequiredContribution(reqContribution);
        dto.setPreferredContribution(prefContribution);

        // Retrieve persisted MatchSkillDetails (immutable historical evidence)
        List<MatchSkillDetail> details = matchSkillDetailRepository.findByMatchResultIdOrderByJobSkillIdAsc(mr.getId());

        List<MatchSkillDetailDto> reqDtos = new ArrayList<>();
        List<MatchSkillDetailDto> prefDtos = new ArrayList<>();

        // Enforce overlap precedence: REQUIRED takes precedence over PREFERRED
        // Each canonical skill appears at most once, and if required, only under REQUIRED
        Set<Long> seenRequiredSkillIds = new HashSet<>();
        Set<Long> seenPreferredSkillIds = new HashSet<>();

        // First pass: collect REQUIRED skills
        for (MatchSkillDetail d : details) {
            if (d.getNecessity() == RequirementNecessity.REQUIRED) {
                Long skillId = d.getSkill() != null ? d.getSkill().getId() : null;
                if (skillId != null && seenRequiredSkillIds.add(skillId)) {
                    reqDtos.add(mapSkillDetailDto(d));
                }
            }
        }

        // Second pass: collect PREFERRED skills (excluding any skill that exists under REQUIRED)
        for (MatchSkillDetail d : details) {
            if (d.getNecessity() != RequirementNecessity.REQUIRED) {
                Long skillId = d.getSkill() != null ? d.getSkill().getId() : null;
                if (skillId != null && !seenRequiredSkillIds.contains(skillId) && seenPreferredSkillIds.add(skillId)) {
                    prefDtos.add(mapSkillDetailDto(d));
                }
            }
        }

        // Deterministic sorting by jobSkillId ASC
        reqDtos.sort(Comparator.comparing(MatchSkillDetailDto::getJobSkillId, Comparator.nullsLast(Comparator.naturalOrder())));
        prefDtos.sort(Comparator.comparing(MatchSkillDetailDto::getJobSkillId, Comparator.nullsLast(Comparator.naturalOrder())));

        dto.setRequiredSkills(reqDtos);
        dto.setPreferredSkills(prefDtos);

        return dto;
    }

    /**
     * Backward-compatible overload for getting match details without explicit caller department.
     */
    @Transactional(readOnly = true)
    public MatchDetailResponseDto getMatchDetail(Long jobId, Long resumeId) {
        return getMatchDetail(jobId, resumeId, null);
    }

    private MatchSkillDetailDto mapSkillDetailDto(MatchSkillDetail d) {
        MatchSkillDetailDto detailDto = new MatchSkillDetailDto();
        detailDto.setId(d.getId());
        detailDto.setJobSkillId(d.getJobSkill() != null ? d.getJobSkill().getId() : null);
        detailDto.setSkillId(d.getSkill() != null ? d.getSkill().getId() : null);
        detailDto.setSkillName(d.getSkill() != null ? d.getSkill().getName() : null);
        detailDto.setNecessity(d.getNecessity() != null ? d.getNecessity().name() : null);
        boolean matched = Boolean.TRUE.equals(d.getIsMatched());
        detailDto.setIsMatched(matched);

        if (matched) {
            detailDto.setCandidateConfidence(d.getCandidateConfidence());
            detailDto.setCandidateMatchedText(DeterministicPiiScrubber.scrubMatchedText(d.getCandidateMatchedText()));
            detailDto.setCandidateContextSnippet(DeterministicPiiScrubber.scrubContextSnippet(d.getCandidateContextSnippet()));
        } else {
            detailDto.setCandidateConfidence(null);
            detailDto.setCandidateMatchedText(null);
            detailDto.setCandidateContextSnippet(null);
        }
        return detailDto;
    }

    /**
     * Get all deterministic match results for a job with department authorization.
     */
    @Transactional(readOnly = true)
    public List<MatchResponseDto> getJobMatches(Long jobId, String callerDepartment) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job with ID " + jobId + " not found."));
        authorizationService.validateJobAccess(job, callerDepartment);

        return matchResultRepository.findByJobIdAndMatchingMethodOrderByOverallScoreDesc(
                jobId, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }

    /**
     * Backward-compatible overload for getting job matches without explicit caller department.
     */
    @Transactional(readOnly = true)
    public List<MatchResponseDto> getJobMatches(Long jobId) {
        return getJobMatches(jobId, null);
    }

    /**
     * Mark existing match results stale when job requirements or candidate skills mutate.
     */
    @Transactional
    public void markStaleForJob(Long jobId) {
        matchResultRepository.markStaleByJobId(jobId);
    }

    @Transactional
    public void markStaleForResume(Long resumeId) {
        matchResultRepository.markStaleByResumeId(resumeId);
    }

    private MatchResponseDto mapToResponseDto(MatchResult mr) {
        MatchResponseDto dto = new MatchResponseDto();
        dto.setMatchId(mr.getId());
        dto.setResumeId(mr.getResume().getId());
        dto.setCandidateId(mr.getCandidate().getId());
        dto.setOverallScore(mr.getOverallScore());
        dto.setRequiredSkillCoverage(mr.getRequiredSkillCoverage());
        dto.setPreferredSkillCoverage(mr.getPreferredSkillCoverage());
        dto.setRequiredSkillsMatched(mr.getRequiredSkillsMatched());
        dto.setRequiredSkillsTotal(mr.getRequiredSkillsTotal());
        dto.setPreferredSkillsMatched(mr.getPreferredSkillsMatched());
        dto.setPreferredSkillsTotal(mr.getPreferredSkillsTotal());
        dto.setIsStale(mr.getIsStale());
        dto.setScoredAt(mr.getScoredAt());
        return dto;
    }

    private ScreeningMode parseScreeningMode(String screeningModeStr) {
        if (screeningModeStr == null || screeningModeStr.isBlank()) {
            return ScreeningMode.NORMAL;
        }
        try {
            return ScreeningMode.valueOf(screeningModeStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidScreeningModeException(
                    "Invalid screening mode '" + screeningModeStr + "'. Allowed modes: NORMAL, BLIND."
            );
        }
    }
}
