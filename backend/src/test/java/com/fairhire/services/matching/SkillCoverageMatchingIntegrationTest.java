package com.fairhire.services.matching;

import com.fairhire.FairHireApplication;
import com.fairhire.dto.MatchBatchResponseDto;
import com.fairhire.dto.MatchDetailResponseDto;
import com.fairhire.dto.MatchResponseDto;
import com.fairhire.dto.MatchSkillDetailDto;
import com.fairhire.models.*;
import com.fairhire.models.enums.*;
import com.fairhire.repositories.*;
import com.fairhire.services.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = FairHireApplication.class)
@DisplayName("Phase 4B: Skill Coverage Matching Integration Tests")
class SkillCoverageMatchingIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private JobSkillRepository jobSkillRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private ResumeSkillRepository resumeSkillRepository;

    @Autowired
    private MatchResultRepository matchResultRepository;

    @Autowired
    private MatchSkillDetailRepository matchSkillDetailRepository;

    @Autowired
    private SkillCoverageMatchingService matchingService;

    @Autowired
    private JobService jobService;

    @Autowired
    private com.fairhire.services.skill.SkillPersistenceService skillPersistenceService;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    private User testRecruiter;
    private Skill skillJava;
    private Skill skillSpring;
    private Skill skillDocker;
    private Skill skillKotlin;

    @BeforeEach
    void setUp() {
        testRecruiter = userRepository.findByEmail("recruiter.phase4b@fairhire.ai")
                .orElseGet(() -> userRepository.save(new User("Phase4 Recruiter", "recruiter.phase4b@fairhire.ai", "hash", UserRole.RECRUITER)));

        skillJava = skillRepository.findByName("Java")
                .orElseGet(() -> skillRepository.save(new Skill("Java", "PROGRAMMING_LANGUAGE")));

        skillSpring = skillRepository.findByName("Spring Boot")
                .orElseGet(() -> skillRepository.save(new Skill("Spring Boot", "FRAMEWORK")));

        skillDocker = skillRepository.findByName("Docker")
                .orElseGet(() -> skillRepository.save(new Skill("Docker", "TOOL")));

        skillKotlin = skillRepository.findByName("Kotlin")
                .orElseGet(() -> skillRepository.save(new Skill("Kotlin", "PROGRAMMING_LANGUAGE")));
    }

    private Job createJobWithSkills(String title, String department, List<Skill> required, List<Skill> preferred) {
        Job job = new Job(title, "Description for " + title, testRecruiter);
        job.setDepartment(department);
        job = jobRepository.save(job);

        for (Skill s : required) {
            jobSkillRepository.save(new JobSkill(job, s, true, 2, new BigDecimal("1.000"), "MANUAL", "Required snippet", s.getName()));
        }
        for (Skill s : preferred) {
            jobSkillRepository.save(new JobSkill(job, s, false, 0, new BigDecimal("1.000"), "MANUAL", "Preferred snippet", s.getName()));
        }
        return jobRepository.findById(job.getId()).orElseThrow();
    }

    private Resume createResumeWithSkills(String candidateName, List<Skill> skills, BigDecimal confidence, String snippet) {
        Candidate candidate = candidateRepository.save(new Candidate(
                candidateName,
                "cand." + System.nanoTime() + "@example.com",
                "555-0100",
                testRecruiter
        ));

        Resume resume = resumeRepository.save(new Resume(
                candidate,
                candidateName.replace(" ", "_") + ".pdf",
                ResumeFileType.PDF,
                "hash_" + System.nanoTime(),
                "Resume content for " + candidateName,
                new BigDecimal("4.0"),
                "BS CS"
        ));
        resume.setParsingStatus(ParsingStatus.READY);
        resume = resumeRepository.save(resume);

        for (Skill s : skills) {
            resumeSkillRepository.save(new ResumeSkill(
                    resume, s, confidence, snippet != null ? snippet : "Experience in " + s.getName(), s.getName(), "EXACT_CANONICAL_MATCH"
            ));
        }
        return resumeRepository.findById(resume.getId()).orElseThrow();
    }

    @Test
    @DisplayName("Oracle 1: PII Isolation in Database - Evidence snippet is scrubbed")
    void testOracle1PiiIsolationInDb() {
        Job job = createJobWithSkills("Java Engineer", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResumeWithSkills(
                "John Doe",
                List.of(skillJava),
                new BigDecimal("0.950"),
                "John Doe, john@example.com, 555-1234, Senior Java Dev"
        );

        MatchBatchResponseDto response = matchingService.matchJob(job.getId(), List.of(resume.getId()), "Engineering", null);
        assertEquals(1, response.getTotalEvaluated());
        assertEquals(0, new BigDecimal("100.00").compareTo(response.getResults().get(0).getOverallScore()));

        MatchDetailResponseDto detail = matchingService.getMatchDetail(job.getId(), resume.getId(), "Engineering");
        assertEquals(1, detail.getRequiredSkills().size());
        String snippet = detail.getRequiredSkills().get(0).getCandidateContextSnippet();

        assertNotNull(snippet);
        assertTrue(snippet.contains("[REDACTED_EMAIL]"));
        assertTrue(snippet.contains("[REDACTED_PHONE]"));
        assertFalse(snippet.contains("john@example.com"));
        assertFalse(snippet.contains("555-1234"));
    }

    @Test
    @DisplayName("Oracle 6: Zero Required Recognized Skills - Aborts with HTTP 422 MATCHING_REQUIREMENTS_NOT_FOUND")
    void testOracle6ZeroRequirements() {
        Job emptyJob = new Job("Empty Job", "No skills defined", testRecruiter);
        emptyJob.setDepartment("Engineering");
        emptyJob = jobRepository.save(emptyJob);

        Resume resume = createResumeWithSkills("Jane Doe", List.of(skillJava), new BigDecimal("0.900"), "Snippet");

        final Long emptyJobId = emptyJob.getId();
        final Long resumeId = resume.getId();
        MatchingRequirementsNotFoundException ex = assertThrows(MatchingRequirementsNotFoundException.class, () -> {
            matchingService.matchJob(emptyJobId, List.of(resumeId), "Engineering", null);
        });

        assertTrue(ex.getMessage().contains("zero recognized canonical skill requirements"));
    }

    @Test
    @DisplayName("Oracle 8: Confidence Does Not Affect Score")
    void testOracle8ConfidenceDoesNotAffectScore() {
        Job job = createJobWithSkills("Java Dev", "Engineering", List.of(skillJava), Collections.emptyList());

        Resume resHighConf = createResumeWithSkills("Cand High", List.of(skillJava), new BigDecimal("0.950"), "Snippet");
        Resume resLowConf = createResumeWithSkills("Cand Low", List.of(skillJava), new BigDecimal("0.500"), "Snippet");

        MatchBatchResponseDto respHigh = matchingService.matchJob(job.getId(), List.of(resHighConf.getId()), "Engineering", null);
        MatchBatchResponseDto respLow = matchingService.matchJob(job.getId(), List.of(resLowConf.getId()), "Engineering", null);

        assertEquals(0, respHigh.getResults().get(0).getOverallScore().compareTo(respLow.getResults().get(0).getOverallScore()));
        assertEquals(0, new BigDecimal("100.00").compareTo(respHigh.getResults().get(0).getOverallScore()));
    }

    @Test
    @DisplayName("Oracle 9: Candidate with Zero Recognized Skills - Score is 0.00")
    void testOracle9ZeroCandidateSkills() {
        Job job = createJobWithSkills("Backend Dev", "Engineering", List.of(skillJava, skillSpring), List.of(skillDocker));
        Resume emptyCandidateResume = createResumeWithSkills("No Skills Cand", Collections.emptyList(), new BigDecimal("0.900"), null);

        MatchBatchResponseDto response = matchingService.matchJob(job.getId(), List.of(emptyCandidateResume.getId()), "Engineering", null);
        MatchResponseDto r = response.getResults().get(0);

        assertEquals(0, new BigDecimal("0.00").compareTo(r.getOverallScore()));
        assertEquals(0, new BigDecimal("0.0000").compareTo(r.getRequiredSkillCoverage()));
        assertEquals(0, new BigDecimal("0.0000").compareTo(r.getPreferredSkillCoverage()));
        assertEquals(0, r.getRequiredSkillsMatched());
        assertEquals(2, r.getRequiredSkillsTotal());
    }

    @Test
    @DisplayName("Oracle 11: Legacy SEMANTIC MatchResult row remains untouched")
    void testOracle11LegacySemanticPreservation() {
        Job job = createJobWithSkills("Legacy Guard Job", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResumeWithSkills("Legacy Guard Cand", List.of(skillJava), new BigDecimal("0.900"), "Snippet");

        // Create historical legacy SEMANTIC match result
        MatchResult legacy = new MatchResult();
        legacy.setJob(job);
        legacy.setCandidate(resume.getCandidate());
        legacy.setResume(resume);
        legacy.setScreeningMode(ScreeningMode.NORMAL);
        legacy.setMatchingMethod(MatchingMethod.SEMANTIC);
        legacy.setSemanticScore(new BigDecimal("0.9450"));
        legacy.setModelVersion("all-MiniLM-L6-v2");
        legacy.setFinalCompositeScore(new BigDecimal("0.9450"));
        legacy = matchResultRepository.saveAndFlush(legacy);

        Long legacyId = legacy.getId();

        // Run deterministic matching
        matchingService.matchJob(job.getId(), List.of(resume.getId()), "Engineering", null);

        // Verify historical SEMANTIC row is intact and untouched
        MatchResult loadedLegacy = matchResultRepository.findById(legacyId).orElseThrow();
        assertEquals(MatchingMethod.SEMANTIC, loadedLegacy.getMatchingMethod());
        assertEquals("all-MiniLM-L6-v2", loadedLegacy.getModelVersion());
        assertEquals(0, new BigDecimal("0.9450").compareTo(loadedLegacy.getSemanticScore()));
        assertNull(loadedLegacy.getRequiredSkillCoverage());

        // Verify distinct deterministic result exists
        MatchResult deterministicResult = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();

        assertNotEquals(legacyId, deterministicResult.getId());
        assertEquals(MatchingMethod.CANONICAL_SKILL_COVERAGE, deterministicResult.getMatchingMethod());
        assertEquals("deterministic-v1", deterministicResult.getAlgorithmVersion());
        assertEquals(0, new BigDecimal("100.00").compareTo(deterministicResult.getOverallScore()));
    }

    @Test
    @DisplayName("Oracle 12: Idempotency - Repeated matching creates no duplicate records")
    void testOracle12Idempotency() {
        Job job = createJobWithSkills("Idempotent Job", "Engineering", List.of(skillJava, skillSpring), List.of(skillDocker));
        Resume resume = createResumeWithSkills("Idempotent Cand", List.of(skillJava), new BigDecimal("0.900"), "Snippet");

        // Run 1
        matchingService.matchJob(job.getId(), List.of(resume.getId()), "Engineering", null);

        // Run 2
        matchingService.matchJob(job.getId(), List.of(resume.getId()), "Engineering", null);

        List<MatchResult> deterministicResults = matchResultRepository.findByJobIdAndMatchingMethod(
                job.getId(), MatchingMethod.CANONICAL_SKILL_COVERAGE
        );
        assertEquals(1, deterministicResults.size(), "Repeated matching must produce exactly one deterministic result");

        List<MatchSkillDetail> details = matchSkillDetailRepository.findByMatchResultId(deterministicResults.get(0).getId());
        assertEquals(3, details.size(), "Detail count must exactly equal job skills count (3)");
    }

    @Test
    @DisplayName("Oracle 13: Detail Completeness - Every JobSkill requirement has exactly one MatchSkillDetail")
    void testOracle13DetailCompleteness() {
        Job job = createJobWithSkills("Completeness Job", "Engineering", List.of(skillJava, skillSpring), List.of(skillDocker));
        Resume resume = createResumeWithSkills("Completeness Cand", List.of(skillJava), new BigDecimal("0.900"), "Snippet");

        matchingService.matchJob(job.getId(), List.of(resume.getId()), "Engineering", null);

        MatchDetailResponseDto detail = matchingService.getMatchDetail(job.getId(), resume.getId(), "Engineering");
        assertEquals(2, detail.getRequiredSkills().size());
        assertEquals(1, detail.getPreferredSkills().size());

        // Verify Java is matched and Spring Boot is not matched
        MatchSkillDetailDto javaDetail = detail.getRequiredSkills().stream()
                .filter(d -> d.getSkillName().equals("Java")).findFirst().orElseThrow();
        assertTrue(javaDetail.getIsMatched());

        MatchSkillDetailDto springDetail = detail.getRequiredSkills().stream()
                .filter(d -> d.getSkillName().equals("Spring Boot")).findFirst().orElseThrow();
        assertFalse(springDetail.getIsMatched());
    }

    @Test
    @DisplayName("Oracle 14: Detail Replacement - Candidate skill changes atomically replace MatchSkillDetails")
    void testOracle14DetailReplacement() {
        Job job = createJobWithSkills("Replacement Job", "Engineering", List.of(skillJava, skillSpring), Collections.emptyList());
        Resume resume = createResumeWithSkills("Replacement Cand", List.of(skillJava), new BigDecimal("0.900"), "Snippet");

        // Initial match: 1 matched (Java) -> 50.00
        matchingService.matchJob(job.getId(), List.of(resume.getId()), "Engineering", null);
        MatchDetailResponseDto initialDetail = matchingService.getMatchDetail(job.getId(), resume.getId(), "Engineering");
        assertEquals(0, new BigDecimal("50.00").compareTo(initialDetail.getOverallScore()));

        // Candidate learns Spring Boot! Add Spring Boot to resume skills
        resumeSkillRepository.save(new ResumeSkill(resume, skillSpring, new BigDecimal("0.920"), "Learned Spring Boot"));

        // Recalculate
        matchingService.matchJob(job.getId(), List.of(resume.getId()), "Engineering", null);
        MatchDetailResponseDto updatedDetail = matchingService.getMatchDetail(job.getId(), resume.getId(), "Engineering");

        assertEquals(0, new BigDecimal("100.00").compareTo(updatedDetail.getOverallScore()));
        assertEquals(2, updatedDetail.getRequiredSkillsMatched());

        List<MatchResult> list = matchResultRepository.findByJobIdAndMatchingMethod(job.getId(), MatchingMethod.CANONICAL_SKILL_COVERAGE);
        assertEquals(1, list.size());

        List<MatchSkillDetail> details = matchSkillDetailRepository.findByMatchResultId(list.get(0).getId());
        assertEquals(2, details.size());
        assertTrue(details.stream().allMatch(MatchSkillDetail::getIsMatched));
    }

    @Test
    @DisplayName("Oracle 15: Stale-Result Invalidation - JobService.updateJob() marks results stale, rematching resets is_stale")
    void testOracle15StaleResultLifecycle() {
        Job job = createJobWithSkills("Stale Test Job", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResumeWithSkills("Stale Cand", List.of(skillJava), new BigDecimal("0.900"), "Snippet");

        // Initial match: 100.00, is_stale = false
        matchingService.matchJob(job.getId(), List.of(resume.getId()), "Engineering", null);
        MatchResult initial = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();
        assertFalse(initial.getIsStale());

        // Recruiter updates job title and description via JobService
        jobService.updateJob(job.getId(), "Senior Java and Kotlin Architect", "Requires Java and Kotlin expertise", "Java, Kotlin");
        jobSkillRepository.save(new JobSkill(job, skillKotlin, true, 2, new BigDecimal("1.000"), "MANUAL", "Kotlin required", "Kotlin"));

        // Check that match result is now marked is_stale = true
        MatchResult staleMatch = matchResultRepository.findById(initial.getId()).orElseThrow();
        assertTrue(staleMatch.getIsStale(), "Job mutation must atomically mark existing match results is_stale = true");

        // Recruiter re-triggers matching
        matchingService.matchJob(job.getId(), List.of(resume.getId()), "Engineering", null);

        // Rematched result: score is updated (1/2 = 50.00) and is_stale is reset to false
        MatchResult rematched = matchResultRepository.findById(initial.getId()).orElseThrow();
        assertFalse(rematched.getIsStale(), "Recalculation must reset is_stale = false");
        assertEquals(0, new BigDecimal("50.00").compareTo(rematched.getOverallScore()));
    }

    @Test
    @DisplayName("Oracle 16 / Resource Authorization: Cross-department matching is blocked with 403 UNAUTHORIZED_RESOURCE_ACCESS")
    void testResourceAuthorizationBoundary() {
        Job financeJob = createJobWithSkills("Finance Analyst", "Finance", List.of(skillJava), Collections.emptyList());
        Resume engineeringResume = createResumeWithSkills("Eng Cand", List.of(skillJava), new BigDecimal("0.900"), "Snippet");

        // Caller claims department "Engineering" trying to access Job in "Finance"
        final Long financeJobId = financeJob.getId();
        final Long resumeId = engineeringResume.getId();
        assertThrows(UnauthorizedResourceAccessException.class, () -> {
            matchingService.matchJob(financeJobId, List.of(resumeId), "Engineering", null);
        });

        // Resume in department "Engineering" trying to match against Job in "Finance"
        assertThrows(UnauthorizedResourceAccessException.class, () -> {
            matchingService.matchJob(financeJobId, List.of(resumeId), "Finance", Map.of(resumeId, "Engineering"));
        });
    }

    @Test
    @DisplayName("Oracle 18: Batch Size Limit - >100 resumes rejected with 422 BATCH_SIZE_LIMIT_EXCEEDED")
    void testBatchSizeLimitExceeded() {
        Job job = createJobWithSkills("Batch Job", "Engineering", List.of(skillJava), Collections.emptyList());

        List<Long> oversizedList = new ArrayList<>();
        for (long i = 1; i <= 101; i++) {
            oversizedList.add(i);
        }

        final Long jobId = job.getId();
        BatchSizeLimitExceededException ex = assertThrows(BatchSizeLimitExceededException.class, () -> {
            matchingService.matchJob(jobId, oversizedList, "Engineering", null);
        });

        assertTrue(ex.getMessage().contains("exceeds maximum synchronous limit (100)"));
    }

    @Test
    @DisplayName("Mandatory A: Job title change marks deterministic result stale")
    void testJobTitleChangeMarksDeterministicResultStale() {
        Job job = createJobWithSkills("Engineer Level 1", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResumeWithSkills("Alice Dev", List.of(skillJava), new BigDecimal("0.900"), "Snippet");

        matchingService.matchJob(job.getId(), List.of(resume.getId()), "Engineering", null);
        MatchResult initial = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();
        assertFalse(initial.getIsStale());

        jobService.updateJobTitle(job.getId(), "Engineer Level 2");

        MatchResult updated = matchResultRepository.findById(initial.getId()).orElseThrow();
        assertTrue(updated.getIsStale(), "Updating job title must mark deterministic match result stale");
    }

    @Test
    @DisplayName("Mandatory B: Job description change marks deterministic result stale")
    void testJobDescriptionChangeMarksDeterministicResultStale() {
        Job job = createJobWithSkills("Backend Lead", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResumeWithSkills("Bob Dev", List.of(skillJava), new BigDecimal("0.900"), "Snippet");

        matchingService.matchJob(job.getId(), List.of(resume.getId()), "Engineering", null);
        MatchResult initial = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();
        assertFalse(initial.getIsStale());

        jobService.updateJobDescription(job.getId(), "Updated description requiring distributed systems expertise");

        MatchResult updated = matchResultRepository.findById(initial.getId()).orElseThrow();
        assertTrue(updated.getIsStale(), "Updating job description must mark deterministic match result stale");
    }

    @Test
    @DisplayName("Mandatory C: Job requirement change marks deterministic result stale")
    void testJobRequirementChangeMarksDeterministicResultStale() {
        Job job = createJobWithSkills("DevOps Eng", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResumeWithSkills("Charlie Dev", List.of(skillJava), new BigDecimal("0.900"), "Snippet");

        matchingService.matchJob(job.getId(), List.of(resume.getId()), "Engineering", null);
        MatchResult initial = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();
        assertFalse(initial.getIsStale());

        jobService.updateJobRequirements(job.getId(), "Must have AWS certification and 5 years experience");

        MatchResult updated = matchResultRepository.findById(initial.getId()).orElseThrow();
        assertTrue(updated.getIsStale(), "Updating job requirements must mark deterministic match result stale");
    }

    @Test
    @DisplayName("Mandatory D: JobSkill necessity change marks deterministic result stale")
    void testJobSkillNecessityChangeMarksDeterministicResultStale() {
        Job job = createJobWithSkills("Fullstack Eng", "Engineering", List.of(skillJava), List.of(skillDocker));
        Resume resume = createResumeWithSkills("David Dev", List.of(skillJava), new BigDecimal("0.900"), "Snippet");

        // Java is required, Docker is preferred. Candidate has Java. Score = 80.00
        matchingService.matchJob(job.getId(), List.of(resume.getId()), "Engineering", null);
        MatchResult initial = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();
        assertFalse(initial.getIsStale());
        assertEquals(0, new BigDecimal("80.00").compareTo(initial.getOverallScore()));

        // Change Docker from preferred to mandatory
        JobSkill dockerSkill = jobSkillRepository.findByJobIdAndSkillId(job.getId(), skillDocker.getId()).orElseThrow();
        jobService.updateJobSkillNecessity(job.getId(), dockerSkill.getId(), true);

        MatchResult staleResult = matchResultRepository.findById(initial.getId()).orElseThrow();
        assertTrue(staleResult.getIsStale(), "Changing JobSkill necessity must mark deterministic match result stale");

        // Re-match: now Java & Docker are required, score is 50.00, is_stale is false
        matchingService.matchJob(job.getId(), List.of(resume.getId()), "Engineering", null);
        MatchResult rematched = matchResultRepository.findById(initial.getId()).orElseThrow();
        assertFalse(rematched.getIsStale());
        assertEquals(0, new BigDecimal("50.00").compareTo(rematched.getOverallScore()));
    }

    @Test
    @DisplayName("Mandatory E: JobSkill canonical skill change marks deterministic result stale")
    void testJobSkillCanonicalSkillChangeMarksDeterministicResultStale() {
        Job job = createJobWithSkills("Language Eng", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResumeWithSkills("Eve Dev", List.of(skillJava), new BigDecimal("0.900"), "Snippet");

        matchingService.matchJob(job.getId(), List.of(resume.getId()), "Engineering", null);
        MatchResult initial = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();
        assertFalse(initial.getIsStale());

        // Change canonical skill from Java to Kotlin
        JobSkill javaJobSkill = jobSkillRepository.findByJobIdAndSkillId(job.getId(), skillJava.getId()).orElseThrow();
        jobService.updateJobSkillCanonicalSkill(job.getId(), javaJobSkill.getId(), skillKotlin.getId());

        MatchResult updated = matchResultRepository.findById(initial.getId()).orElseThrow();
        assertTrue(updated.getIsStale(), "Changing JobSkill canonical skill must mark deterministic match result stale");
    }

    @Test
    @DisplayName("Mandatory F: Resume skill extraction/reparse marks deterministic result stale")
    void testResumeSkillExtractionMarksDeterministicResultStale() {
        Job job = createJobWithSkills("Polyglot Eng", "Engineering", List.of(skillJava, skillKotlin), Collections.emptyList());
        Resume resume = createResumeWithSkills("Frank Dev", List.of(skillJava), new BigDecimal("0.900"), "Snippet");

        matchingService.matchJob(job.getId(), List.of(resume.getId()), "Engineering", null);
        MatchResult initial = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();
        assertFalse(initial.getIsStale());

        // Resume re-extraction occurs: candidate now possesses both Java and Kotlin
        skillPersistenceService.replaceSkills(resume, List.of(
                new com.fairhire.services.skill.ExtractedSkillCandidate(skillJava, skillJava.getName(), "Java", "Java snippet", new BigDecimal("0.950"), SkillExtractionMethod.EXACT_CANONICAL_MATCH, 0, 4),
                new com.fairhire.services.skill.ExtractedSkillCandidate(skillKotlin, skillKotlin.getName(), "Kotlin", "Kotlin snippet", new BigDecimal("0.910"), SkillExtractionMethod.EXACT_CANONICAL_MATCH, 5, 11)
        ));

        MatchResult staleResult = matchResultRepository.findById(initial.getId()).orElseThrow();
        assertTrue(staleResult.getIsStale(), "Resume skill re-extraction must mark deterministic match result stale");

        // Recalculate: resets is_stale to false and score updates to 100.00
        matchingService.matchJob(job.getId(), List.of(resume.getId()), "Engineering", null);
        MatchResult rematched = matchResultRepository.findById(initial.getId()).orElseThrow();
        assertFalse(rematched.getIsStale());
        assertEquals(0, new BigDecimal("100.00").compareTo(rematched.getOverallScore()));
    }

    @Test
    @DisplayName("Mandatory J: MatchResult deletion cannot leave orphan MatchSkillDetails")
    void testMatchResultDeletionLeavesNoOrphanDetails() {
        Job job = createJobWithSkills("Cascade Clean Job", "Engineering", List.of(skillJava, skillSpring), List.of(skillDocker));
        Resume resume = createResumeWithSkills("Grace Dev", List.of(skillJava), new BigDecimal("0.900"), "Snippet");

        matchingService.matchJob(job.getId(), List.of(resume.getId()), "Engineering", null);
        MatchResult mr = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();

        Long matchResultId = mr.getId();
        List<MatchSkillDetail> detailsBefore = matchSkillDetailRepository.findByMatchResultId(matchResultId);
        assertEquals(3, detailsBefore.size(), "Must have 3 MatchSkillDetails");

        // Delete MatchResult
        matchResultRepository.delete(mr);
        matchResultRepository.flush();

        // Verify MatchResult is deleted
        assertTrue(matchResultRepository.findById(matchResultId).isEmpty());

        // Verify that NO orphan MatchSkillDetail remains for that matchResultId
        List<MatchSkillDetail> detailsAfter = matchSkillDetailRepository.findByMatchResultId(matchResultId);
        assertTrue(detailsAfter.isEmpty(), "MatchResult deletion must leave ZERO orphan MatchSkillDetails");
    }

    @Test
    @DisplayName("Mandatory K: Transaction rollback leaves no partial MatchSkillDetails")
    void testTransactionRollbackLeavesNoPartialDetails() {
        Job job = createJobWithSkills("Rollback Job", "Engineering", List.of(skillJava, skillSpring), Collections.emptyList());
        Resume resume = createResumeWithSkills("Heidi Dev", List.of(skillJava), new BigDecimal("0.900"), "Snippet");

        // Initial count of details in database
        long initialDetailCount = matchSkillDetailRepository.count();

        // Execute a simulated transactional match that throws an exception during processing
        assertThrows(RuntimeException.class, () -> {
            org.springframework.transaction.support.TransactionTemplate txTemplate =
                    new org.springframework.transaction.support.TransactionTemplate(transactionManager);
            txTemplate.execute(status -> {
                matchingService.matchJob(job.getId(), List.of(resume.getId()), "Engineering", null);
                throw new RuntimeException("Simulated unexpected failure before commit");
            });
        });

        // Verify that detail count is identical to initial (zero partial records persisted)
        long postRollbackDetailCount = matchSkillDetailRepository.count();
        assertEquals(initialDetailCount, postRollbackDetailCount, "Rollback must leave zero partial MatchSkillDetails");
    }
}
