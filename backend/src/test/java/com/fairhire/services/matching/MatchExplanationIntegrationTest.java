package com.fairhire.services.matching;

import com.fairhire.FairHireApplication;
import com.fairhire.dto.MatchDetailResponseDto;
import com.fairhire.dto.MatchResponseDto;
import com.fairhire.dto.MatchSkillDetailDto;
import com.fairhire.models.*;
import com.fairhire.models.enums.*;
import com.fairhire.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = FairHireApplication.class)
@DisplayName("Phase 4C: Match Explainability and Audit Verification Tests (Oracles E1 - E14)")
class MatchExplanationIntegrationTest {

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

    private User testRecruiter;
    private Skill skillJava;
    private Skill skillSpring;
    private Skill skillDocker;
    private Skill skillAws;

    @BeforeEach
    void setUp() {
        testRecruiter = userRepository.findByEmail("recruiter.explain@fairhire.ai")
                .orElseGet(() -> userRepository.save(new User("Explain Recruiter", "recruiter.explain@fairhire.ai", "hash", UserRole.RECRUITER)));

        skillJava = skillRepository.findByName("Java")
                .orElseGet(() -> skillRepository.save(new Skill("Java", "PROGRAMMING_LANGUAGE")));

        skillSpring = skillRepository.findByName("Spring Boot")
                .orElseGet(() -> skillRepository.save(new Skill("Spring Boot", "FRAMEWORK")));

        skillDocker = skillRepository.findByName("Docker")
                .orElseGet(() -> skillRepository.save(new Skill("Docker", "TOOL")));

        skillAws = skillRepository.findByName("AWS")
                .orElseGet(() -> skillRepository.save(new Skill("AWS", "CLOUD")));
    }

    private Job createJob(String title, String dept, List<Skill> required, List<Skill> preferred) {
        Job job = jobRepository.save(new Job(title, "Description for " + title, testRecruiter));
        job.setDepartment(dept);
        job = jobRepository.save(job);

        for (Skill s : required) {
            jobSkillRepository.save(new JobSkill(job, s, true, 2, new BigDecimal("1.000"), "MANUAL", "Required " + s.getName(), s.getName()));
        }
        for (Skill s : preferred) {
            jobSkillRepository.save(new JobSkill(job, s, false, 0, new BigDecimal("1.000"), "MANUAL", "Preferred " + s.getName(), s.getName()));
        }
        return job;
    }

    private Resume createResume(String candidateName, List<Skill> skills) {
        Candidate c = candidateRepository.save(new Candidate(candidateName, "cand." + System.nanoTime() + "@fairhire.ai", "555-0199", testRecruiter));
        Resume r = resumeRepository.save(new Resume(c, "resume.pdf", ResumeFileType.PDF, "hash_" + System.nanoTime(), "Resume content", new BigDecimal("4.0"), "BS"));
        r.setParsingStatus(ParsingStatus.READY);
        r = resumeRepository.save(r);

        for (Skill s : skills) {
            resumeSkillRepository.save(new ResumeSkill(r, s, new BigDecimal("0.900"), "Experienced with " + s.getName(), s.getName(), "EXACT_CANONICAL_MATCH"));
        }
        return r;
    }

    @Test
    @DisplayName("Oracle E1: Perfect Match Explanation (all required & preferred skills matched)")
    void testOracleE1_PerfectMatchExplanation() {
        Job job = createJob("Senior Backend Engineer", "Engineering", List.of(skillJava, skillSpring), List.of(skillDocker, skillAws));
        Resume resume = createResume("Alice Perfect", List.of(skillJava, skillSpring, skillDocker, skillAws));

        matchingService.matchSingleCandidate(job.getId(), resume.getId());

        MatchDetailResponseDto explanation = matchingService.getMatchDetail(job.getId(), resume.getId(), "Engineering");

        assertNotNull(explanation);
        assertEquals(new BigDecimal("100.00"), explanation.getOverallScore());
        assertEquals(2, explanation.getRequiredSkillsTotal());
        assertEquals(2, explanation.getRequiredSkillsMatched());
        assertEquals(new BigDecimal("1.0000"), explanation.getRequiredSkillCoverage());
        assertEquals(2, explanation.getPreferredSkillsTotal());
        assertEquals(2, explanation.getPreferredSkillsMatched());
        assertEquals(new BigDecimal("1.0000"), explanation.getPreferredSkillCoverage());

        assertEquals(new BigDecimal("0.80"), explanation.getRequiredWeight());
        assertEquals(new BigDecimal("0.20"), explanation.getPreferredWeight());
        assertEquals(new BigDecimal("80.00"), explanation.getRequiredContribution());
        assertEquals(new BigDecimal("20.00"), explanation.getPreferredContribution());

        assertFalse(explanation.getIsStale());
        assertEquals("ACTIVE", explanation.getStatus());
        assertEquals("deterministic-v1", explanation.getAlgorithmVersion());
        assertEquals("CANONICAL_SKILL_COVERAGE", explanation.getMatchingMethod());
        assertNotNull(explanation.getScoredAt());

        assertEquals(2, explanation.getRequiredSkills().size());
        assertEquals(2, explanation.getPreferredSkills().size());

        for (MatchSkillDetailDto req : explanation.getRequiredSkills()) {
            assertTrue(req.getIsMatched());
            assertTrue(req.getMatched());
            assertNotNull(req.getCandidateConfidence());
            assertNotNull(req.getCandidateMatchedText());
            assertNotNull(req.getCandidateContextSnippet());
        }
        for (MatchSkillDetailDto pref : explanation.getPreferredSkills()) {
            assertTrue(pref.getIsMatched());
            assertTrue(pref.getMatched());
            assertNotNull(pref.getCandidateConfidence());
            assertNotNull(pref.getCandidateMatchedText());
            assertNotNull(pref.getCandidateContextSnippet());
        }
    }

    @Test
    @DisplayName("Oracle E2: Missing Required Skill (matched = false and evidence = null)")
    void testOracleE2_MissingRequiredSkill() {
        Job job = createJob("Backend Dev", "Engineering", List.of(skillJava, skillDocker), Collections.emptyList());
        Resume resume = createResume("Bob JavaOnly", List.of(skillJava)); // missing Docker

        matchingService.matchSingleCandidate(job.getId(), resume.getId());

        MatchDetailResponseDto explanation = matchingService.getMatchDetail(job.getId(), resume.getId(), "Engineering");

        assertNotNull(explanation);
        assertEquals(2, explanation.getRequiredSkillsTotal());
        assertEquals(1, explanation.getRequiredSkillsMatched());
        assertEquals(new BigDecimal("0.5000"), explanation.getRequiredSkillCoverage());
        assertEquals(new BigDecimal("50.00"), explanation.getOverallScore());

        List<MatchSkillDetailDto> reqSkills = explanation.getRequiredSkills();
        assertEquals(2, reqSkills.size());

        MatchSkillDetailDto javaSkill = reqSkills.stream().filter(s -> s.getSkillName().equals("Java")).findFirst().orElseThrow();
        assertTrue(javaSkill.getIsMatched());
        assertTrue(javaSkill.getMatched());
        assertNotNull(javaSkill.getCandidateConfidence());
        assertNotNull(javaSkill.getCandidateMatchedText());

        MatchSkillDetailDto dockerSkill = reqSkills.stream().filter(s -> s.getSkillName().equals("Docker")).findFirst().orElseThrow();
        assertFalse(dockerSkill.getIsMatched());
        assertFalse(dockerSkill.getMatched());
        assertNull(dockerSkill.getCandidateConfidence());
        assertNull(dockerSkill.getCandidateMatchedText());
        assertNull(dockerSkill.getCandidateContextSnippet());
        assertNull(dockerSkill.getMatchedText());
        assertNull(dockerSkill.getContextSnippet());
    }

    @Test
    @DisplayName("Oracle E3: Mixed Required and Preferred Skills properly separated")
    void testOracleE3_MixedRequiredPreferredSeparation() {
        Job job = createJob("Fullstack Dev", "Engineering", List.of(skillJava), List.of(skillAws));
        Resume resume = createResume("Charlie Mixed", List.of(skillJava, skillAws));

        matchingService.matchSingleCandidate(job.getId(), resume.getId());

        MatchDetailResponseDto explanation = matchingService.getMatchDetail(job.getId(), resume.getId(), "Engineering");

        assertEquals(1, explanation.getRequiredSkills().size());
        assertEquals("Java", explanation.getRequiredSkills().get(0).getSkillName());
        assertEquals("REQUIRED", explanation.getRequiredSkills().get(0).getNecessity());

        assertEquals(1, explanation.getPreferredSkills().size());
        assertEquals("AWS", explanation.getPreferredSkills().get(0).getSkillName());
        assertEquals("PREFERRED", explanation.getPreferredSkills().get(0).getNecessity());
    }

    @Test
    @DisplayName("Oracle E4: Overlap Precedence (skill exists in both categories appears only under REQUIRED)")
    void testOracleE4_OverlapPrecedence() {
        Job job = createJob("Overlap Job", "Engineering", List.of(skillJava), List.of(skillSpring));
        Candidate cand = candidateRepository.save(new Candidate("David Overlap", "david." + System.nanoTime() + "@fairhire.ai", "555-0104", testRecruiter));
        Resume resume = resumeRepository.save(new Resume(cand, "resume.pdf", ResumeFileType.PDF, "hash_" + System.nanoTime(), "Resume", new BigDecimal("3.0"), "BS"));
        resume.setParsingStatus(ParsingStatus.READY);
        resume = resumeRepository.save(resume);

        MatchResult mr = new MatchResult();
        mr.setJob(job);
        mr.setCandidate(cand);
        mr.setResume(resume);
        mr.setScreeningMode(ScreeningMode.NORMAL);
        mr.setMatchingMethod(MatchingMethod.CANONICAL_SKILL_COVERAGE);
        mr.setOverallScore(new BigDecimal("100.00"));
        mr.setRequiredSkillCoverage(new BigDecimal("1.0000"));
        mr.setPreferredSkillCoverage(new BigDecimal("1.0000"));
        mr.setRequiredSkillsTotal(1);
        mr.setRequiredSkillsMatched(1);
        mr.setPreferredSkillsTotal(1);
        mr.setPreferredSkillsMatched(1);
        mr.setIsStale(false);
        mr.setAlgorithmVersion("deterministic-v1");
        mr.setScoredAt(Instant.now());
        mr = matchResultRepository.save(mr);

        List<JobSkill> jobSkills = jobSkillRepository.findByJobId(job.getId());
        JobSkill js1 = jobSkills.stream().filter(js -> Boolean.TRUE.equals(js.getIsMandatory())).findFirst().orElseThrow();
        JobSkill js2 = jobSkills.stream().filter(js -> Boolean.FALSE.equals(js.getIsMandatory())).findFirst().orElseThrow();

        // detail1: Java marked REQUIRED
        MatchSkillDetail detail1 = new MatchSkillDetail(
                mr, js1, skillJava, RequirementNecessity.REQUIRED, true,
                new BigDecimal("0.900"), "Java snippet", "Context for Java"
        );
        matchSkillDetailRepository.save(detail1);

        // detail2: Java also associated with js2 but marked PREFERRED (simulating category overlap)
        MatchSkillDetail detail2 = new MatchSkillDetail(
                mr, js2, skillJava, RequirementNecessity.PREFERRED, true,
                new BigDecimal("0.900"), "Java snippet", "Context for Java"
        );
        matchSkillDetailRepository.save(detail2);

        MatchDetailResponseDto explanation = matchingService.getMatchDetail(job.getId(), resume.getId(), "Engineering");

        // Java must appear under REQUIRED
        assertEquals(1, explanation.getRequiredSkills().size());
        assertEquals("Java", explanation.getRequiredSkills().get(0).getSkillName());

        // Java must NOT appear in preferredSkills list (preferredSkills should be empty)
        assertTrue(explanation.getPreferredSkills().isEmpty());
    }

    @Test
    @DisplayName("Oracle E5: PII Safety (persisted evidence with email, phone, URL, address exposed only sanitized)")
    void testOracleE5_PiiSafety() {
        Job job = createJob("PII Test Job", "Engineering", List.of(skillJava), Collections.emptyList());
        Candidate cand = candidateRepository.save(new Candidate("Eva PII", "eva.test@fairhire.ai", "555-0999", testRecruiter));
        Resume resume = resumeRepository.save(new Resume(cand, "resume.pdf", ResumeFileType.PDF, "hash_" + System.nanoTime(), "Resume text", new BigDecimal("4.0"), "BS"));
        resume.setParsingStatus(ParsingStatus.READY);
        resume = resumeRepository.save(resume);

        MatchResult mr = new MatchResult();
        mr.setJob(job);
        mr.setCandidate(cand);
        mr.setResume(resume);
        mr.setScreeningMode(ScreeningMode.NORMAL);
        mr.setMatchingMethod(MatchingMethod.CANONICAL_SKILL_COVERAGE);
        mr.setOverallScore(new BigDecimal("100.00"));
        mr.setRequiredSkillCoverage(new BigDecimal("1.0000"));
        mr.setRequiredSkillsTotal(1);
        mr.setRequiredSkillsMatched(1);
        mr.setPreferredSkillsTotal(0);
        mr.setPreferredSkillsMatched(0);
        mr.setIsStale(false);
        mr.setAlgorithmVersion("deterministic-v1");
        mr.setScoredAt(Instant.now());
        mr = matchResultRepository.save(mr);

        JobSkill js = jobSkillRepository.findByJobId(job.getId()).get(0);

        // Manually persist MatchSkillDetail containing raw PII to test explanation defense-in-depth sanitization
        MatchSkillDetail detail = new MatchSkillDetail(
                mr, js, skillJava, RequirementNecessity.REQUIRED, true,
                new BigDecimal("0.950"),
                "Java expert email: john.doe@example.com",
                "Contact at 555-123-4567, visit https://linkedin.com/in/johndoe, lives at 123 Elm Street"
        );
        matchSkillDetailRepository.save(detail);

        MatchDetailResponseDto explanation = matchingService.getMatchDetail(job.getId(), resume.getId(), "Engineering");

        assertNotNull(explanation);
        MatchSkillDetailDto skillDto = explanation.getRequiredSkills().get(0);

        assertFalse(skillDto.getCandidateMatchedText().contains("john.doe@example.com"));
        assertTrue(skillDto.getCandidateMatchedText().contains("[REDACTED_EMAIL]"));

        assertFalse(skillDto.getCandidateContextSnippet().contains("555-123-4567"));
        assertTrue(skillDto.getCandidateContextSnippet().contains("[REDACTED_PHONE]"));

        assertFalse(skillDto.getCandidateContextSnippet().contains("https://linkedin.com/in/johndoe"));
        assertTrue(skillDto.getCandidateContextSnippet().contains("[REDACTED_URL]"));

        assertFalse(skillDto.getCandidateContextSnippet().contains("123 Elm Street"));
        assertTrue(skillDto.getCandidateContextSnippet().contains("[REDACTED_ADDRESS]"));
    }

    @Test
    @DisplayName("Oracle E6: No Raw Resume Access (explanation uses MatchSkillDetail evidence without reparsing raw text)")
    void testOracleE6_NoRawResumeAccess() {
        Job job = createJob("Senior SRE", "Engineering", List.of(skillDocker), Collections.emptyList());
        Resume resume = createResume("Frank SRE", List.of(skillDocker));

        matchingService.matchSingleCandidate(job.getId(), resume.getId());

        // Corrupt raw text of resume and delete ResumeSkill records from DB
        resume.setRawText("Completely Corrupted and Irrelevant Text That Mentions No Skills 12345");
        resumeRepository.save(resume);
        resumeSkillRepository.deleteAll(resumeSkillRepository.findByResumeId(resume.getId()));

        // Explanation must succeed and provide evidence solely from MatchSkillDetail
        MatchDetailResponseDto explanation = matchingService.getMatchDetail(job.getId(), resume.getId(), "Engineering");

        assertNotNull(explanation);
        assertEquals(1, explanation.getRequiredSkills().size());
        assertEquals("Docker", explanation.getRequiredSkills().get(0).getSkillName());
        assertNotNull(explanation.getRequiredSkills().get(0).getCandidateMatchedText());
    }

    @Test
    @DisplayName("Oracle E7: Stale Result Explanation (isStale = true, status = STALE, no recalculation on GET)")
    void testOracleE7_StaleResultExplanation() {
        Job job = createJob("Lead Architect", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResume("Grace Architect", List.of(skillJava));

        matchingService.matchSingleCandidate(job.getId(), resume.getId());

        // Mark match result stale
        MatchResult mr = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();
        mr.setIsStale(true);
        matchResultRepository.save(mr);

        MatchDetailResponseDto explanation = matchingService.getMatchDetail(job.getId(), resume.getId(), "Engineering");

        assertTrue(explanation.getIsStale());
        assertEquals("STALE", explanation.getStatus());
        assertEquals(new BigDecimal("100.00"), explanation.getOverallScore());

        // Confirm database still has isStale = true (no recalculation occurred)
        MatchResult reloaded = matchResultRepository.findById(mr.getId()).orElseThrow();
        assertTrue(reloaded.getIsStale());
    }

    @Test
    @DisplayName("Oracle E8: Historical Explanation (retains MatchSkillDetail evidence even if current skills mutate)")
    void testOracleE8_HistoricalExplanation() {
        Job job = createJob("Historical Engineer", "Engineering", List.of(skillJava, skillSpring), Collections.emptyList());
        Resume resume = createResume("Hank Historical", List.of(skillJava, skillSpring));

        matchingService.matchSingleCandidate(job.getId(), resume.getId());

        // Mutate current resume skills (remove Spring Boot from candidate profile)
        ResumeSkill rsSpring = resumeSkillRepository.findByResumeId(resume.getId()).stream()
                .filter(rs -> rs.getSkill().getName().equals("Spring Boot"))
                .findFirst().orElseThrow();
        resumeSkillRepository.delete(rsSpring);

        // Fetch old MatchResult explanation
        MatchDetailResponseDto explanation = matchingService.getMatchDetail(job.getId(), resume.getId(), "Engineering");

        // Historical explanation must still reflect the 2 matched skills that existed when scored
        assertEquals(2, explanation.getRequiredSkillsMatched());
        assertEquals(new BigDecimal("100.00"), explanation.getOverallScore());
        assertEquals(2, explanation.getRequiredSkills().size());
        assertTrue(explanation.getRequiredSkills().stream().allMatch(MatchSkillDetailDto::getIsMatched));
    }

    @Test
    @DisplayName("Oracle E9: Legacy SEMANTIC result explanation (no canonical details fabricated, scores untouched)")
    void testOracleE9_LegacySemanticResult() {
        Job job = jobRepository.save(new Job("Semantic Job", "Desc", testRecruiter));
        job.setDepartment("Engineering");
        job = jobRepository.save(job);

        Candidate cand = candidateRepository.save(new Candidate("Iris Semantic", "iris." + System.nanoTime() + "@fairhire.ai", "555-0888", testRecruiter));
        Resume resume = resumeRepository.save(new Resume(cand, "resume.pdf", ResumeFileType.PDF, "hash_" + System.nanoTime(), "Resume text", new BigDecimal("2.0"), "BA"));
        resume.setParsingStatus(ParsingStatus.READY);
        resume = resumeRepository.save(resume);

        MatchResult mr = new MatchResult();
        mr.setJob(job);
        mr.setCandidate(cand);
        mr.setResume(resume);
        mr.setScreeningMode(ScreeningMode.NORMAL);
        mr.setMatchingMethod(MatchingMethod.SEMANTIC);
        mr.setSemanticScore(new BigDecimal("0.7850"));
        mr.setModelVersion("all-MiniLM-L6-v2");
        mr.setFinalCompositeScore(new BigDecimal("0.7850"));
        mr.setIsStale(false);
        mr = matchResultRepository.save(mr);

        MatchDetailResponseDto explanation = matchingService.getMatchDetail(job.getId(), resume.getId(), "Engineering");

        assertNotNull(explanation);
        assertEquals("SEMANTIC", explanation.getMatchingMethod());
        assertEquals("all-MiniLM-L6-v2", explanation.getAlgorithmVersion());
        assertEquals(new BigDecimal("78.50"), explanation.getOverallScore());
        assertTrue(explanation.getRequiredSkills().isEmpty());
        assertTrue(explanation.getPreferredSkills().isEmpty());

        // Verify database row remained completely untouched
        MatchResult reloaded = matchResultRepository.findById(mr.getId()).orElseThrow();
        assertEquals(new BigDecimal("0.7850"), reloaded.getSemanticScore());
        assertEquals("all-MiniLM-L6-v2", reloaded.getModelVersion());
    }

    @Test
    @DisplayName("Oracle E10-A: Authorized Department (Caller department matches Job.department -> Success)")
    void testOracleE10A_AuthorizedDepartment() {
        Job job = createJob("Security Engineer", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResume("Jack Sec", List.of(skillJava));
        matchingService.matchSingleCandidate(job.getId(), resume.getId());

        // Authorized caller department matches job department
        MatchDetailResponseDto detail = matchingService.getMatchDetail(job.getId(), resume.getId(), "Engineering");
        assertNotNull(detail);
        assertEquals(job.getId(), detail.getJobId());

        List<MatchResponseDto> list = matchingService.getJobMatches(job.getId(), "Engineering");
        assertNotNull(list);
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("Oracle E10-B: Wrong Department (Caller department differs from Job.department -> 403 UNAUTHORIZED_RESOURCE_ACCESS)")
    void testOracleE10B_WrongDepartment() {
        Job job = createJob("Security Engineer", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResume("Jack Sec", List.of(skillJava));
        matchingService.matchSingleCandidate(job.getId(), resume.getId());

        // Cross-department access is strictly prohibited
        assertThrows(UnauthorizedResourceAccessException.class, () ->
                matchingService.getMatchDetail(job.getId(), resume.getId(), "HumanResources")
        );
        assertThrows(UnauthorizedResourceAccessException.class, () ->
                matchingService.getJobMatches(job.getId(), "HumanResources")
        );
    }

    @Test
    @DisplayName("Oracle E10-C: Missing Caller Authorization Context (null/blank caller department -> 403 UNAUTHORIZED_RESOURCE_ACCESS)")
    void testOracleE10C_MissingCallerAuthorization() {
        Job job = createJob("Security Engineer", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResume("Jack Sec", List.of(skillJava));
        matchingService.matchSingleCandidate(job.getId(), resume.getId());

        // Null caller department fails closed
        assertThrows(UnauthorizedResourceAccessException.class, () ->
                matchingService.getMatchDetail(job.getId(), resume.getId(), null)
        );
        assertThrows(UnauthorizedResourceAccessException.class, () ->
                matchingService.getJobMatches(job.getId(), null)
        );

        // Blank caller department fails closed
        assertThrows(UnauthorizedResourceAccessException.class, () ->
                matchingService.getMatchDetail(job.getId(), resume.getId(), "   ")
        );
        assertThrows(UnauthorizedResourceAccessException.class, () ->
                matchingService.getJobMatches(job.getId(), "   ")
        );
    }

    @Test
    @DisplayName("Oracle E10-D: Cannot Derive Authorization from Protected Job (Missing caller cannot fall back to job.department)")
    void testOracleE10D_CannotDeriveAuthorizationFromProtectedJob() {
        Job job = createJob("Security Engineer", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResume("Jack Sec", List.of(skillJava));
        matchingService.matchSingleCandidate(job.getId(), resume.getId());

        // The endpoint and service cannot derive authorization from the job's own department
        // Invariant: resource.department != caller.department when caller is unauthenticated
        assertThrows(UnauthorizedResourceAccessException.class, () ->
                matchingService.getMatchDetail(job.getId(), resume.getId())
        );
        assertThrows(UnauthorizedResourceAccessException.class, () ->
                matchingService.getJobMatches(job.getId())
        );
    }

    @Test
    @DisplayName("Oracle E11: Not Found Scenarios (404 on unknown job, resume, or match)")
    void testOracleE11_NotFoundScenarios() {
        Job job = createJob("Found Job", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResume("Found Resume", List.of(skillJava));

        // Unknown job
        assertThrows(ResourceNotFoundException.class, () ->
                matchingService.getMatchDetail(999999L, resume.getId(), "Engineering")
        );

        // Unknown resume
        assertThrows(ResourceNotFoundException.class, () ->
                matchingService.getMatchDetail(job.getId(), 999999L, "Engineering")
        );

        // Known job and resume, but no match generated
        assertThrows(ResourceNotFoundException.class, () ->
                matchingService.getMatchDetail(job.getId(), resume.getId(), "Engineering")
        );
    }

    @Test
    @DisplayName("Oracle E12: Deterministic Ordering (repeated GET calls return identical skill order)")
    void testOracleE12_DeterministicOrdering() {
        Job job = createJob("Order Test Job", "Engineering", List.of(skillJava, skillSpring, skillDocker), Collections.emptyList());
        Resume resume = createResume("Kelly Order", List.of(skillJava, skillSpring, skillDocker));

        matchingService.matchSingleCandidate(job.getId(), resume.getId());

        MatchDetailResponseDto call1 = matchingService.getMatchDetail(job.getId(), resume.getId(), "Engineering");
        MatchDetailResponseDto call2 = matchingService.getMatchDetail(job.getId(), resume.getId(), "Engineering");
        MatchDetailResponseDto call3 = matchingService.getMatchDetail(job.getId(), resume.getId(), "Engineering");

        assertEquals(call1.getRequiredSkills().size(), call2.getRequiredSkills().size());
        assertEquals(call2.getRequiredSkills().size(), call3.getRequiredSkills().size());

        for (int i = 0; i < call1.getRequiredSkills().size(); i++) {
            assertEquals(call1.getRequiredSkills().get(i).getJobSkillId(), call2.getRequiredSkills().get(i).getJobSkillId());
            assertEquals(call2.getRequiredSkills().get(i).getJobSkillId(), call3.getRequiredSkills().get(i).getJobSkillId());
            assertEquals(call1.getRequiredSkills().get(i).getSkillName(), call2.getRequiredSkills().get(i).getSkillName());
        }
    }

    @Test
    @DisplayName("Oracle E13: Read Only Behavior (GET creates no rows and mutates no state)")
    void testOracleE13_ReadOnlyBehavior() {
        Job job = createJob("Read Only Job", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResume("Leo ReadOnly", List.of(skillJava));

        matchingService.matchSingleCandidate(job.getId(), resume.getId());

        long matchResultsCountBefore = matchResultRepository.count();
        long skillDetailsCountBefore = matchSkillDetailRepository.count();

        // Perform GET explanation
        MatchDetailResponseDto explanation = matchingService.getMatchDetail(job.getId(), resume.getId(), "Engineering");
        assertNotNull(explanation);

        long matchResultsCountAfter = matchResultRepository.count();
        long skillDetailsCountAfter = matchSkillDetailRepository.count();

        assertEquals(matchResultsCountBefore, matchResultsCountAfter);
        assertEquals(skillDetailsCountBefore, skillDetailsCountAfter);
    }

    @Test
    @DisplayName("Oracle E14: Score Authority (persisted MatchResult score is authoritative and never recomputed)")
    void testOracleE14_ScoreAuthority() {
        Job job = createJob("Authority Job", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResume("Mona Authority", List.of(skillJava));

        matchingService.matchSingleCandidate(job.getId(), resume.getId());

        MatchResult mr = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();

        // Artificially change the persisted score in MatchResult to 77.77 to prove authority
        mr.setOverallScore(new BigDecimal("77.77"));
        matchResultRepository.save(mr);

        // Add extra skills to candidate resume
        resumeSkillRepository.save(new ResumeSkill(resume, skillSpring, new BigDecimal("0.900"), "Extra Spring", "Spring", "EXACT"));

        MatchDetailResponseDto explanation = matchingService.getMatchDetail(job.getId(), resume.getId(), "Engineering");

        // Returned score must be the persisted authoritative score (77.77), NOT recomputed from current skills
        assertEquals(new BigDecimal("77.77"), explanation.getOverallScore());
    }
}
