package com.fairhire.services.matching;

import com.fairhire.FairHireApplication;
import com.fairhire.models.*;
import com.fairhire.models.enums.*;
import com.fairhire.repositories.*;
import com.fairhire.services.JobService;
import com.fairhire.services.skill.ExtractedSkillCandidate;
import com.fairhire.services.skill.SkillPersistenceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.LongStream;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 4D End-to-End Integration QA Test Suite.
 * Validates the complete Phase 4 matching lifecycle from Job/Resume ingestion
 * through POST matching, persistence, GET explainability, staleness mutations,
 * recalculation, authorization, concurrency safety, and database integrity.
 */
@SpringBootTest(classes = FairHireApplication.class)
@AutoConfigureMockMvc
@DisplayName("Phase 4D: End-to-End Integration QA Test Suite")
class Phase4EndToEndIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private JobService jobService;

    @Autowired
    private SkillPersistenceService skillPersistenceService;

    @Autowired
    private SkillCoverageMatchingService matchingService;

    @Autowired
    private com.fairhire.config.JwtUtils jwtUtils;

    private String token;

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authPost(String url, Object... uriVars) {
        return post(url, uriVars).header("Authorization", "Bearer " + token);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authGet(String url, Object... uriVars) {
        return get(url, uriVars).header("Authorization", "Bearer " + token);
    }

    private User testRecruiter;
    private Skill skillJava;
    private Skill skillSpring;
    private Skill skillDocker;
    private Skill skillKotlin;

    @BeforeEach
    void setUp() {
        testRecruiter = userRepository.findByEmail("recruiter.p4d@fairhire.ai")
                .orElseGet(() -> userRepository.save(new User("P4D Recruiter", "recruiter.p4d@fairhire.ai", "hash", UserRole.RECRUITER)));
        token = jwtUtils.generateToken(testRecruiter.getId(), testRecruiter.getEmail(), "RECRUITER", "Engineering");

        skillJava = skillRepository.findByName("Java")
                .orElseGet(() -> skillRepository.save(new Skill("Java", "PROGRAMMING_LANGUAGE")));

        skillSpring = skillRepository.findByName("Spring Boot")
                .orElseGet(() -> skillRepository.save(new Skill("Spring Boot", "FRAMEWORK")));

        skillDocker = skillRepository.findByName("Docker")
                .orElseGet(() -> skillRepository.save(new Skill("Docker", "DEVOPS")));

        skillKotlin = skillRepository.findByName("Kotlin")
                .orElseGet(() -> skillRepository.save(new Skill("Kotlin", "PROGRAMMING_LANGUAGE")));
    }

    private Job createJob(String title, String department, List<Skill> requiredSkills, List<Skill> preferredSkills) {
        Job job = new Job(title, "Role description for " + title, testRecruiter);
        job.setDepartment(department);
        job = jobRepository.save(job);

        for (Skill s : requiredSkills) {
            jobSkillRepository.save(new JobSkill(job, s, true, 2, new BigDecimal("1.000"), "MANUAL", "Required " + s.getName(), s.getName()));
        }
        for (Skill s : preferredSkills) {
            jobSkillRepository.save(new JobSkill(job, s, false, 1, new BigDecimal("1.000"), "MANUAL", "Preferred " + s.getName(), s.getName()));
        }
        return jobRepository.save(job);
    }

    private Resume createResume(String candidateName, List<Skill> skills, String snippet) {
        Candidate candidate = candidateRepository.save(new Candidate(candidateName, "p4d." + System.nanoTime() + "@fairhire.ai", "555-0199", testRecruiter));
        Resume resume = new Resume(candidate, "resume.pdf", ResumeFileType.PDF, "hash_" + System.nanoTime(), "Resume text for " + candidateName, new BigDecimal("4.0"), "BS CS");
        resume.setParsingStatus(ParsingStatus.READY);
        resume = resumeRepository.save(resume);

        for (Skill s : skills) {
            resumeSkillRepository.save(new ResumeSkill(resume, s, new BigDecimal("0.900"), snippet, s.getName(), "EXACT_CANONICAL_MATCH"));
        }
        return resume;
    }

    // ========================================================================
    // T01: Successful Deterministic Match
    // ========================================================================
    @Test
    @DisplayName("T01: Successful deterministic match creates MatchResult and MatchSkillDetails")
    void testT01SuccessfulDeterministicMatch() throws Exception {
        Job job = createJob("P4D Software Engineer", "Engineering", List.of(skillJava), List.of(skillDocker));
        Resume resume = createResume("T01 Candidate", List.of(skillJava), "Experienced Java backend developer");

        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job_id", is(job.getId().intValue())))
                .andExpect(jsonPath("$.algorithm_version", is("deterministic-v1")))
                .andExpect(jsonPath("$.matching_method", is("CANONICAL_SKILL_COVERAGE")))
                .andExpect(jsonPath("$.total_evaluated", is(1)))
                .andExpect(jsonPath("$.results[0].overall_score", is(80.0)))
                .andExpect(jsonPath("$.results[0].required_skills_matched", is(1)))
                .andExpect(jsonPath("$.results[0].required_skills_total", is(1)))
                .andExpect(jsonPath("$.results[0].preferred_skills_matched", is(0)))
                .andExpect(jsonPath("$.results[0].preferred_skills_total", is(1)));

        // Verify persisted MatchResult
        MatchResult mr = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();
        assertEquals(0, new BigDecimal("80.00").compareTo(mr.getOverallScore()));
        assertEquals(0, new BigDecimal("1.0000").compareTo(mr.getRequiredSkillCoverage()));
        assertEquals(0, new BigDecimal("0.0000").compareTo(mr.getPreferredSkillCoverage()));
        assertFalse(mr.getIsStale());
        assertNotNull(mr.getScoredAt());

        // Verify persisted MatchSkillDetails
        List<MatchSkillDetail> details = matchSkillDetailRepository.findByMatchResultId(mr.getId());
        assertEquals(2, details.size());

        MatchSkillDetail javaDetail = details.stream().filter(d -> d.getSkill().getId().equals(skillJava.getId())).findFirst().orElseThrow();
        assertTrue(javaDetail.getIsMatched());
        assertEquals(RequirementNecessity.REQUIRED, javaDetail.getNecessity());
        assertEquals(0, new BigDecimal("0.900").compareTo(javaDetail.getCandidateConfidence()));

        MatchSkillDetail dockerDetail = details.stream().filter(d -> d.getSkill().getId().equals(skillDocker.getId())).findFirst().orElseThrow();
        assertFalse(dockerDetail.getIsMatched());
        assertEquals(RequirementNecessity.PREFERRED, dockerDetail.getNecessity());
        assertNull(dockerDetail.getCandidateConfidence());
    }

    // ========================================================================
    // T02: Unknown Job
    // ========================================================================
    @Test
    @DisplayName("T02: Unknown Job returns 404 RESOURCE_NOT_FOUND")
    void testT02UnknownJob() throws Exception {
        mockMvc.perform(authPost("/api/v1/jobs/999999/matches")
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [1]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("RESOURCE_NOT_FOUND")));
    }

    // ========================================================================
    // T03: Unknown Resume
    // ========================================================================
    @Test
    @DisplayName("T03: Unknown Resume returns 404 RESOURCE_NOT_FOUND")
    void testT03UnknownResume() throws Exception {
        Job job = createJob("T03 Job", "Engineering", List.of(skillJava), Collections.emptyList());

        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [999999]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("RESOURCE_NOT_FOUND")));
    }

    // ========================================================================
    // T04: Zero Recognized Required Skills
    // ========================================================================
    @Test
    @DisplayName("T04: Zero recognized required skills returns 422 MATCHING_REQUIREMENTS_NOT_FOUND")
    void testT04ZeroRecognizedRequiredSkills() throws Exception {
        // Job with only preferred skill, no required skills
        Job job = createJob("T04 Job No Required", "Engineering", Collections.emptyList(), List.of(skillDocker));
        Resume resume = createResume("T04 Candidate", List.of(skillDocker), "Docker dev");

        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status", is(422)))
                .andExpect(jsonPath("$.error", is("MATCHING_REQUIREMENTS_NOT_FOUND")));

        // Verify NO match result created
        List<MatchResult> results = matchResultRepository.findByJobIdAndMatchingMethod(job.getId(), MatchingMethod.CANONICAL_SKILL_COVERAGE);
        assertTrue(results.isEmpty(), "Zero-requirement job must not persist MatchResult rows");
    }

    // ========================================================================
    // T05: Candidate with Zero Skills
    // ========================================================================
    @Test
    @DisplayName("T05: Candidate with zero skills produces valid match with score 0.00")
    void testT05CandidateWithZeroSkills() throws Exception {
        Job job = createJob("T05 Job", "Engineering", List.of(skillJava), List.of(skillDocker));
        Resume resume = createResume("T05 Zero Skill Cand", Collections.emptyList(), "Unrelated background");

        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].overall_score", is(0.0)))
                .andExpect(jsonPath("$.results[0].required_skills_matched", is(0)))
                .andExpect(jsonPath("$.results[0].preferred_skills_matched", is(0)));

        MatchResult mr = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(mr.getOverallScore()));
        assertEquals(0, BigDecimal.ZERO.compareTo(mr.getRequiredSkillCoverage()));
        assertEquals(0, BigDecimal.ZERO.compareTo(mr.getPreferredSkillCoverage()));

        List<MatchSkillDetail> details = matchSkillDetailRepository.findByMatchResultId(mr.getId());
        assertEquals(2, details.size());
        assertTrue(details.stream().noneMatch(MatchSkillDetail::getIsMatched));
    }

    // ========================================================================
    // T06: Authorized POST
    // ========================================================================
    @Test
    @DisplayName("T06: Authorized POST succeeds with 200 OK")
    void testT06AuthorizedPost() throws Exception {
        Job job = createJob("T06 Auth Job", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResume("T06 Cand", List.of(skillJava), "Java dev");

        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isOk());
    }

    // ========================================================================
    // T07: Unauthorized POST
    // ========================================================================
    @Test
    @DisplayName("T07: Wrong department POST returns 403 UNAUTHORIZED_RESOURCE_ACCESS")
    void testT07UnauthorizedPost() throws Exception {
        Job job = createJob("T07 Eng Job", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResume("T07 Cand", List.of(skillJava), "Java dev");

        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Finance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED_RESOURCE_ACCESS")));
    }

    // ========================================================================
    // T08: Missing Authorization
    // ========================================================================
    @Test
    @DisplayName("T08: Missing or blank authorization fails closed with 403 on POST, GET list, and GET detail")
    void testT08MissingAuthorization() throws Exception {
        Job job = createJob("T08 Auth Boundary Job", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResume("T08 Cand", List.of(skillJava), "Java dev");
        String noDeptToken = jwtUtils.generateToken(testRecruiter.getId(), testRecruiter.getEmail(), "RECRUITER");

        // POST without department authorization
        mockMvc.perform(post("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("Authorization", "Bearer " + noDeptToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED_RESOURCE_ACCESS")));

        // POST with blank header
        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "   ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));

        // GET list without department authorization
        mockMvc.perform(get("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("Authorization", "Bearer " + noDeptToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED_RESOURCE_ACCESS")));

        // GET detail without department authorization
        mockMvc.perform(get("/api/v1/jobs/{jobId}/matches/{resumeId}", job.getId(), resume.getId())
                        .header("Authorization", "Bearer " + noDeptToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED_RESOURCE_ACCESS")));
    }

    // ========================================================================
    // T09: Duplicate / Idempotent Match
    // ========================================================================
    @Test
    @DisplayName("T09: Duplicate POST match does not create duplicate MatchResult or MatchSkillDetail rows")
    void testT09DuplicateIdempotentMatch() throws Exception {
        Job job = createJob("T09 Idempotent Job", "Engineering", List.of(skillJava, skillSpring), List.of(skillDocker));
        Resume resume = createResume("T09 Cand", List.of(skillJava), "Java dev");

        // Run 1
        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isOk());

        // Run 2
        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isOk());

        List<MatchResult> deterministicResults = matchResultRepository.findByJobIdAndMatchingMethod(
                job.getId(), MatchingMethod.CANONICAL_SKILL_COVERAGE
        );
        assertEquals(1, deterministicResults.size(), "Idempotent matching must produce exactly one MatchResult");

        List<MatchSkillDetail> details = matchSkillDetailRepository.findByMatchResultId(deterministicResults.get(0).getId());
        assertEquals(3, details.size(), "Detail count must remain exactly 3 (one per JobSkill)");
    }

    // ========================================================================
    // T10: GET List Consistency
    // ========================================================================
    @Test
    @DisplayName("T10: GET match list exposes identical score and counts as POST response")
    void testT10GetListConsistency() throws Exception {
        Job job = createJob("T10 List Job", "Engineering", List.of(skillJava), List.of(skillDocker));
        Resume resume = createResume("T10 Cand", List.of(skillJava, skillDocker), "Java & Docker expert");

        // POST match
        MvcResult postResult = mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode postJson = objectMapper.readTree(postResult.getResponse().getContentAsString());
        double postScore = postJson.get("results").get(0).get("overall_score").asDouble();

        // GET list
        mockMvc.perform(authGet("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_evaluated", is(1)))
                .andExpect(jsonPath("$.results[0].overall_score", is(postScore)))
                .andExpect(jsonPath("$.results[0].required_skills_matched", is(1)))
                .andExpect(jsonPath("$.results[0].preferred_skills_matched", is(1)));
    }

    // ========================================================================
    // T11: GET Detail Consistency
    // ========================================================================
    @Test
    @DisplayName("T11: GET match detail exposes identical scores, coverages, and counts without recalculation")
    void testT11GetDetailConsistency() throws Exception {
        Job job = createJob("T11 Detail Job", "Engineering", List.of(skillJava, skillSpring), List.of(skillDocker));
        Resume resume = createResume("T11 Cand", List.of(skillJava), "Java dev");

        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isOk());

        mockMvc.perform(authGet("/api/v1/jobs/{jobId}/matches/{resumeId}", job.getId(), resume.getId())
                        .header("X-Department", "Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overall_score", is(40.0)))
                .andExpect(jsonPath("$.required_skills_matched", is(1)))
                .andExpect(jsonPath("$.required_skills_total", is(2)))
                .andExpect(jsonPath("$.preferred_skills_matched", is(0)))
                .andExpect(jsonPath("$.preferred_skills_total", is(1)))
                .andExpect(jsonPath("$.is_stale", is(false)))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.algorithm_version", is("deterministic-v1")))
                .andExpect(jsonPath("$.matching_method", is("CANONICAL_SKILL_COVERAGE")));
    }

    // ========================================================================
    // T12: Job Title Stale
    // ========================================================================
    @Test
    @DisplayName("T12: Job title mutation marks deterministic result stale")
    void testT12JobTitleStale() throws Exception {
        Job job = createJob("T12 Junior Dev", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResume("T12 Cand", List.of(skillJava), "Java dev");

        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isOk());

        jobService.updateJobTitle(job.getId(), "T12 Senior Architect");

        mockMvc.perform(authGet("/api/v1/jobs/{jobId}/matches/{resumeId}", job.getId(), resume.getId())
                        .header("X-Department", "Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_stale", is(true)))
                .andExpect(jsonPath("$.status", is("STALE")));
    }

    // ========================================================================
    // T13: Job Description Stale
    // ========================================================================
    @Test
    @DisplayName("T13: Job description mutation marks deterministic result stale")
    void testT13JobDescriptionStale() throws Exception {
        Job job = createJob("T13 Job", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResume("T13 Cand", List.of(skillJava), "Java dev");

        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isOk());

        jobService.updateJobDescription(job.getId(), "New mutated description with advanced requirements");

        MatchResult mr = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();
        assertTrue(mr.getIsStale(), "Job description mutation must mark match result stale");
    }

    // ========================================================================
    // T14: Explicit Requirement Stale
    // ========================================================================
    @Test
    @DisplayName("T14: Explicit requirement mutation marks deterministic result stale")
    void testT14ExplicitRequirementStale() throws Exception {
        Job job = createJob("T14 Job", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResume("T14 Cand", List.of(skillJava), "Java dev");

        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isOk());

        jobService.updateJobRequirements(job.getId(), "Must hold AWS Solutions Architect certification");

        MatchResult mr = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();
        assertTrue(mr.getIsStale(), "Explicit requirements mutation must mark match result stale");
    }

    // ========================================================================
    // T15: JobSkill Necessity Stale
    // ========================================================================
    @Test
    @DisplayName("T15: JobSkill necessity change marks deterministic result stale")
    void testT15JobSkillNecessityStale() throws Exception {
        Job job = createJob("T15 Job", "Engineering", List.of(skillJava), List.of(skillDocker));
        Resume resume = createResume("T15 Cand", List.of(skillJava), "Java dev");

        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isOk());

        JobSkill dockerJs = jobSkillRepository.findByJobIdAndSkillId(job.getId(), skillDocker.getId()).orElseThrow();
        jobService.updateJobSkillNecessity(job.getId(), dockerJs.getId(), true);

        MatchResult mr = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();
        assertTrue(mr.getIsStale(), "Changing JobSkill necessity must mark match result stale");
    }

    // ========================================================================
    // T16: JobSkill Canonical Skill Stale
    // ========================================================================
    @Test
    @DisplayName("T16: JobSkill canonical skill change marks deterministic result stale")
    void testT16JobSkillCanonicalSkillStale() throws Exception {
        Job job = createJob("T16 Job", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResume("T16 Cand", List.of(skillJava), "Java dev");

        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isOk());

        JobSkill javaJs = jobSkillRepository.findByJobIdAndSkillId(job.getId(), skillJava.getId()).orElseThrow();
        jobService.updateJobSkillCanonicalSkill(job.getId(), javaJs.getId(), skillKotlin.getId());

        MatchResult mr = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();
        assertTrue(mr.getIsStale(), "Changing JobSkill canonical skill must mark match result stale");
    }

    // ========================================================================
    // T17: JobSkill Add Stale
    // ========================================================================
    @Test
    @DisplayName("T17: Adding JobSkill marks deterministic result stale")
    void testT17JobSkillAddStale() throws Exception {
        Job job = createJob("T17 Job", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResume("T17 Cand", List.of(skillJava), "Java dev");

        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isOk());

        jobService.addJobSkill(job.getId(), skillDocker.getId(), false, 1, new BigDecimal("1.000"));

        MatchResult mr = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();
        assertTrue(mr.getIsStale(), "Adding JobSkill must mark match result stale");
    }

    // ========================================================================
    // T18: JobSkill Remove Stale
    // ========================================================================
    @Test
    @DisplayName("T18: Removing JobSkill marks deterministic result stale")
    void testT18JobSkillRemoveStale() throws Exception {
        Job job = createJob("T18 Job", "Engineering", List.of(skillJava), List.of(skillDocker));
        Resume resume = createResume("T18 Cand", List.of(skillJava), "Java dev");

        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isOk());

        JobSkill dockerJs = jobSkillRepository.findByJobIdAndSkillId(job.getId(), skillDocker.getId()).orElseThrow();
        jobService.removeJobSkill(job.getId(), dockerJs.getId());

        MatchResult mr = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();
        assertTrue(mr.getIsStale(), "Removing JobSkill must mark match result stale");
    }

    // ========================================================================
    // T19: Resume Skill Replacement Stale
    // ========================================================================
    @Test
    @DisplayName("T19: Resume skill replacement marks deterministic result stale")
    void testT19ResumeSkillReplacementStale() throws Exception {
        Job job = createJob("T19 Job", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResume("T19 Cand", List.of(skillJava), "Java dev");

        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isOk());

        skillPersistenceService.replaceSkills(resume, List.of(
                new ExtractedSkillCandidate(skillJava, "Java", "Java", "Updated Java snippet", new BigDecimal("0.980"), SkillExtractionMethod.EXACT_CANONICAL_MATCH, 0, 4),
                new ExtractedSkillCandidate(skillKotlin, "Kotlin", "Kotlin", "Kotlin snippet", new BigDecimal("0.950"), SkillExtractionMethod.EXACT_CANONICAL_MATCH, 5, 11)
        ));

        MatchResult mr = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();
        assertTrue(mr.getIsStale(), "Replacing resume skills must mark match result stale");
    }

    // ========================================================================
    // T20: Explicit Recalculation
    // ========================================================================
    @Test
    @DisplayName("T20: Explicit recalculation resets is_stale, updates score and scored_at, replaces details atomically")
    void testT20ExplicitRecalculation() throws Exception {
        Job job = createJob("T20 Recalc Job", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResume("T20 Cand", List.of(skillJava), "Java dev");

        // Initial match: 100.00
        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isOk());

        MatchResult initial = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();
        Instant initialScoredAt = initial.getScoredAt();

        // Mutate job: add Kotlin as mandatory requirement -> is_stale = true
        jobSkillRepository.save(new JobSkill(job, skillKotlin, true, 2, new BigDecimal("1.000"), "MANUAL", "Kotlin", "Kotlin"));
        jobService.markJobMatchesStale(job.getId());

        MatchResult staleMatch = matchResultRepository.findById(initial.getId()).orElseThrow();
        assertTrue(staleMatch.getIsStale());

        // Explicit POST match to recalculate
        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].overall_score", is(50.0)))
                .andExpect(jsonPath("$.results[0].required_skills_matched", is(1)))
                .andExpect(jsonPath("$.results[0].required_skills_total", is(2)));

        MatchResult rematched = matchResultRepository.findById(initial.getId()).orElseThrow();
        assertFalse(rematched.getIsStale(), "Recalculation must reset is_stale to false");
        assertEquals(0, new BigDecimal("50.00").compareTo(rematched.getOverallScore()));
        assertTrue(rematched.getScoredAt().isAfter(initialScoredAt) || rematched.getScoredAt().equals(initialScoredAt));

        // Details must be atomically replaced (now 2 details: Java matched, Kotlin unmatched)
        List<MatchSkillDetail> details = matchSkillDetailRepository.findByMatchResultId(rematched.getId());
        assertEquals(2, details.size(), "Details count must match 2 evaluated job skills");
    }

    // ========================================================================
    // T21: Historical Result Integrity
    // ========================================================================
    @Test
    @DisplayName("T21: Historical result persisted explanation remains unchanged upon subsequent Job/Resume mutations")
    void testT21HistoricalResultIntegrity() throws Exception {
        Job job = createJob("T21 History Job", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResume("T21 Cand", List.of(skillJava), "Java dev");

        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isOk());

        MatchResult mr = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();
        Long matchResultId = mr.getId();

        // Mutate job skills directly
        jobSkillRepository.save(new JobSkill(job, skillSpring, true, 3, new BigDecimal("1.000"), "MANUAL", "Spring", "Spring"));
        jobService.markJobMatchesStale(job.getId());

        // Verify that the historical MatchSkillDetail count for matchResultId is STILL 1 (Java), NOT 2
        List<MatchSkillDetail> historicalDetails = matchSkillDetailRepository.findByMatchResultId(matchResultId);
        assertEquals(1, historicalDetails.size(), "Historical MatchSkillDetails must not be retroactively altered by job skill mutations");

        MatchResult currentMr = matchResultRepository.findById(matchResultId).orElseThrow();
        assertEquals(0, new BigDecimal("100.00").compareTo(currentMr.getOverallScore()), "Historical persisted score must remain unchanged");
        assertTrue(currentMr.getIsStale(), "Only is_stale flag changes");
    }

    // ========================================================================
    // T22: Legacy SEMANTIC Preservation
    // ========================================================================
    @Test
    @DisplayName("T22: Legacy SEMANTIC match results are preserved and untouched by deterministic matching")
    void testT22LegacySemanticPreservation() throws Exception {
        Job job = createJob("T22 Legacy Job", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResume("T22 Legacy Cand", List.of(skillJava), "Java dev");

        // Insert historical SEMANTIC match result
        MatchResult legacy = new MatchResult();
        legacy.setJob(job);
        legacy.setCandidate(resume.getCandidate());
        legacy.setResume(resume);
        legacy.setScreeningMode(ScreeningMode.NORMAL);
        legacy.setMatchingMethod(MatchingMethod.SEMANTIC);
        legacy.setSemanticScore(new BigDecimal("0.9123"));
        legacy.setModelVersion("all-MiniLM-L6-v2");
        legacy.setFinalCompositeScore(new BigDecimal("0.9123"));
        legacy = matchResultRepository.saveAndFlush(legacy);
        Long legacyId = legacy.getId();

        // Run deterministic POST matching
        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isOk());

        // Verify historical SEMANTIC row is intact
        MatchResult loadedLegacy = matchResultRepository.findById(legacyId).orElseThrow();
        assertEquals(MatchingMethod.SEMANTIC, loadedLegacy.getMatchingMethod());
        assertEquals("all-MiniLM-L6-v2", loadedLegacy.getModelVersion());
        assertEquals(0, new BigDecimal("0.9123").compareTo(loadedLegacy.getSemanticScore()));
        assertNull(loadedLegacy.getRequiredSkillCoverage());

        // Verify distinct deterministic result exists
        MatchResult deterministicResult = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();
        assertNotEquals(legacyId, deterministicResult.getId());
        assertEquals(MatchingMethod.CANONICAL_SKILL_COVERAGE, deterministicResult.getMatchingMethod());
        assertEquals("deterministic-v1", deterministicResult.getAlgorithmVersion());
    }

    // ========================================================================
    // T23: Batch Size 1
    // ========================================================================
    @Test
    @DisplayName("T23: Batch size 1 candidate succeeds synchronously")
    void testT23BatchSize1() throws Exception {
        Job job = createJob("T23 Job", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResume("T23 Cand", List.of(skillJava), "Java dev");

        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_evaluated", is(1)));
    }

    // ========================================================================
    // T24: Batch Size 100
    // ========================================================================
    @Test
    @DisplayName("T24: Batch size 100 candidates succeeds synchronously")
    void testT24BatchSize100() throws Exception {
        Job job = createJob("T24 Batch 100 Job", "Engineering", List.of(skillJava), Collections.emptyList());

        List<Candidate> candidates = new ArrayList<>();
        List<Resume> resumes = new ArrayList<>();
        List<Long> resumeIds = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            Candidate c = new Candidate("BatchCand_" + i, "batch" + i + "." + System.nanoTime() + "@fairhire.ai", "555-0000", testRecruiter);
            candidates.add(c);
        }
        candidateRepository.saveAll(candidates);

        for (int i = 0; i < 100; i++) {
            Resume r = new Resume(candidates.get(i), "resume.pdf", ResumeFileType.PDF, "hash_batch_" + i, "Batch text", new BigDecimal("3.0"), "BS");
            r.setParsingStatus(ParsingStatus.READY);
            resumes.add(r);
        }
        resumeRepository.saveAll(resumes);

        for (Resume r : resumes) {
            resumeIds.add(r.getId());
        }

        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": " + objectMapper.writeValueAsString(resumeIds) + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_evaluated", is(100)));
    }

    // ========================================================================
    // T25: Batch Size 101 Rejected
    // ========================================================================
    @Test
    @DisplayName("T25: Batch size 101 candidates is rejected with 422 BATCH_SIZE_LIMIT_EXCEEDED")
    void testT25BatchSize101Rejected() throws Exception {
        Job job = createJob("T25 Oversized Job", "Engineering", List.of(skillJava), Collections.emptyList());
        List<Long> oversizedList = LongStream.rangeClosed(1, 101).boxed().toList();

        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": " + objectMapper.writeValueAsString(oversizedList) + "}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status", is(422)))
                .andExpect(jsonPath("$.error", is("BATCH_SIZE_LIMIT_EXCEEDED")));
    }

    // ========================================================================
    // T26: Deterministic Repeatability
    // ========================================================================
    @Test
    @DisplayName("T26: Repeated evaluation of identical inputs yields strictly identical results and ordering")
    void testT26DeterministicRepeatability() throws Exception {
        Job job = createJob("T26 Repeat Job", "Engineering", List.of(skillJava, skillSpring), List.of(skillDocker));
        Resume resume = createResume("T26 Cand", List.of(skillJava), "Java dev");

        Double expectedScore = null;
        Integer expectedMatched = null;

        for (int run = 0; run < 5; run++) {
            MvcResult res = mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                            .header("X-Department", "Engineering")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
            double score = root.get("results").get(0).get("overall_score").asDouble();
            int matched = root.get("results").get(0).get("required_skills_matched").asInt();

            if (expectedScore == null) {
                expectedScore = score;
                expectedMatched = matched;
            } else {
                assertEquals(expectedScore, score, "Run " + run + " score must match first run");
                assertEquals(expectedMatched, matched, "Run " + run + " matched count must match first run");
            }
        }
    }

    // ========================================================================
    // T27: PII End-to-End Safety
    // ========================================================================
    @Test
    @DisplayName("T27: PII is scrubbed before persistence and redacted in explanation APIs")
    void testT27PiiEndToEndSafety() throws Exception {
        Job job = createJob("T27 PII Job", "Engineering", List.of(skillJava), Collections.emptyList());

        String rawPiiSnippet = "Senior Developer (john.secret@example.com, 555-867-5309, https://linkedin.com/in/secret, 742 Evergreen Terrace, Springfield 90210) working on Java";
        Candidate cand = candidateRepository.save(new Candidate("Secret Cand", "secret.p4d." + System.nanoTime() + "@fairhire.ai", "555-867-5309", testRecruiter));
        Resume resume = new Resume(cand, "pii.pdf", ResumeFileType.PDF, "hash_pii_" + System.nanoTime(), rawPiiSnippet, new BigDecimal("3.0"), "BS");
        resume.setParsingStatus(ParsingStatus.READY);
        resume = resumeRepository.save(resume);

        resumeSkillRepository.save(new ResumeSkill(resume, skillJava, new BigDecimal("0.950"), rawPiiSnippet, "Java", "EXACT_CANONICAL_MATCH"));

        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isOk());

        // Verify persisted MatchSkillDetail in database does NOT contain raw PII
        MatchResult mr = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();

        List<MatchSkillDetail> details = matchSkillDetailRepository.findByMatchResultId(mr.getId());
        assertEquals(1, details.size());
        String persistedSnippet = details.get(0).getCandidateContextSnippet();

        assertNotNull(persistedSnippet);
        assertFalse(persistedSnippet.contains("john.secret@example.com"));
        assertFalse(persistedSnippet.contains("555-867-5309"));
        assertFalse(persistedSnippet.contains("https://linkedin.com/in/secret"));
        assertFalse(persistedSnippet.contains("742 Evergreen Terrace"));
        assertFalse(persistedSnippet.contains("90210"));
        assertTrue(persistedSnippet.contains("[REDACTED_EMAIL]"));
        assertTrue(persistedSnippet.contains("[REDACTED_PHONE]"));
        assertTrue(persistedSnippet.contains("[REDACTED_URL]"));
        assertTrue(persistedSnippet.contains("[REDACTED_ADDRESS]"));
        assertTrue(persistedSnippet.length() <= 300);

        // Verify GET explanation output also contains no raw PII
        MvcResult getResult = mockMvc.perform(authGet("/api/v1/jobs/{jobId}/matches/{resumeId}", job.getId(), resume.getId())
                        .header("X-Department", "Engineering"))
                .andExpect(status().isOk())
                .andReturn();

        String getContent = getResult.getResponse().getContentAsString();
        assertFalse(getContent.contains("john.secret@example.com"));
        assertFalse(getContent.contains("555-867-5309"));
        assertFalse(getContent.contains("https://linkedin.com/in/secret"));
        assertFalse(getContent.contains("742 Evergreen Terrace"));
    }

    // ========================================================================
    // T28: GET Read-Only Guarantee
    // ========================================================================
    @Test
    @DisplayName("T28: GET requests are strictly read-only and never mutate database state")
    void testT28GetReadOnlyGuarantee() throws Exception {
        Job job = createJob("T28 Read-Only Job", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResume("T28 Cand", List.of(skillJava), "Java dev");

        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isOk());

        long matchResultsBefore = matchResultRepository.count();
        long matchDetailsBefore = matchSkillDetailRepository.count();
        long jobsBefore = jobRepository.count();
        long resumesBefore = resumeRepository.count();

        MatchResult initialMr = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();
        Instant scoredAtBefore = initialMr.getScoredAt();
        BigDecimal scoreBefore = initialMr.getOverallScore();
        Boolean isStaleBefore = initialMr.getIsStale();

        // Perform GET operations multiple times
        mockMvc.perform(authGet("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering"))
                .andExpect(status().isOk());

        mockMvc.perform(authGet("/api/v1/jobs/{jobId}/matches/{resumeId}", job.getId(), resume.getId())
                        .header("X-Department", "Engineering"))
                .andExpect(status().isOk());

        // Verify database counts and fields are completely unchanged
        assertEquals(matchResultsBefore, matchResultRepository.count(), "GET must not change MatchResult count");
        assertEquals(matchDetailsBefore, matchSkillDetailRepository.count(), "GET must not change MatchSkillDetail count");
        assertEquals(jobsBefore, jobRepository.count(), "GET must not change Job count");
        assertEquals(resumesBefore, resumeRepository.count(), "GET must not change Resume count");

        MatchResult afterMr = matchResultRepository.findById(initialMr.getId()).orElseThrow();
        assertEquals(scoredAtBefore, afterMr.getScoredAt(), "GET must not modify scored_at timestamp");
        assertEquals(0, scoreBefore.compareTo(afterMr.getOverallScore()), "GET must not modify overall_score");
        assertEquals(isStaleBefore, afterMr.getIsStale(), "GET must not modify is_stale flag");
    }

    // ========================================================================
    // T29: Concurrent Identical Matching
    // ========================================================================
    @Test
    @DisplayName("T29: Concurrent identical matching requests serialize safely without duplicate records or race failures")
    void testT29ConcurrentIdenticalMatching() throws Exception {
        Job job = createJob("T29 Concurrency Job", "Engineering", List.of(skillJava, skillSpring), List.of(skillDocker));
        Resume resume = createResume("T29 Cand", List.of(skillJava), "Java dev");

        int concurrency = 5;
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrency);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < concurrency; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    MvcResult mvcRes = mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                                    .header("X-Department", "Engineering")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                            .andReturn();
                    if (mvcRes.getResponse().getStatus() == 200) {
                        successCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Concurrent matching should complete within timeout");
        assertEquals(0, errorCount.get(), "Zero concurrent requests should produce errors");
        assertEquals(concurrency, successCount.get(), "All concurrent requests should return 200 OK");

        List<MatchResult> results = matchResultRepository.findByJobIdAndMatchingMethod(
                job.getId(), MatchingMethod.CANONICAL_SKILL_COVERAGE
        );
        assertEquals(1, results.size(), "Concurrent execution must result in exactly 1 MatchResult");

        List<MatchSkillDetail> details = matchSkillDetailRepository.findByMatchResultId(results.get(0).getId());
        assertEquals(3, details.size(), "Concurrent execution must result in exactly 3 MatchSkillDetails");
    }

    // ========================================================================
    // T30: API Error Contract
    // ========================================================================
    @Test
    @DisplayName("T30: API error responses return structured JSON and no stack traces")
    void testT30ApiErrorContract() throws Exception {
        Job job = createJob("T30 Error Contract Job", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResume("T30 Cand", List.of(skillJava), "Java dev");

        // 400 Bad Request — Invalid screening mode
        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "], \"screening_mode\": \"INVALID_MODE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("INVALID_SCREENING_MODE")))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());

        // 403 Forbidden — Unauthorized department
        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Finance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED_RESOURCE_ACCESS")))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());

        // 404 Not Found — Unknown job
        mockMvc.perform(authPost("/api/v1/jobs/999999/matches")
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());

        // 422 Unprocessable Entity — Invalid matching method
        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "], \"matching_method\": \"SEMANTIC\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status", is(422)))
                .andExpect(jsonPath("$.error", is("INVALID_MATCHING_METHOD")))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    // ========================================================================
    // T31: Response Serialization
    // ========================================================================
    @Test
    @DisplayName("T31: Response serialization supports snake_case and camelCase aliases and exact precision")
    void testT31ResponseSerialization() throws Exception {
        Job job = createJob("T31 Serialization Job", "Engineering", List.of(skillJava), List.of(skillDocker));
        Resume resume = createResume("T31 Cand", List.of(skillJava), "Java dev");

        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isOk());

        MvcResult detailRes = mockMvc.perform(authGet("/api/v1/jobs/{jobId}/matches/{resumeId}", job.getId(), resume.getId())
                        .header("X-Department", "Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overall_score", is(80.0)))
                .andExpect(jsonPath("$.required_skill_coverage", is(1.0)))
                .andExpect(jsonPath("$.preferred_skill_coverage", is(0.0)))
                .andExpect(jsonPath("$.is_stale", is(false)))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andReturn();

        String body = detailRes.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);

        assertTrue(json.has("overall_score"));
        assertTrue(json.has("required_skill_coverage"));
        assertTrue(json.has("preferred_skill_coverage"));
        assertTrue(json.has("is_stale"));
        assertTrue(json.get("is_stale").isBoolean());
        assertFalse(json.get("is_stale").asBoolean());
    }

    // ========================================================================
    // T32: Database Integrity
    // ========================================================================
    @Test
    @DisplayName("T32: Database integrity verifies V1-V5 migration files exist, no V6, and cascade deletion")
    void testT32DatabaseIntegrity() throws Exception {
        Path migrationDir = Paths.get("src/main/resources/db/migration");
        assertTrue(Files.exists(migrationDir.resolve("V1__initial_schema.sql")), "V1 migration must exist");
        assertTrue(Files.exists(migrationDir.resolve("V2__resume_ingestion_lifecycle.sql")), "V2 migration must exist");
        assertTrue(Files.exists(migrationDir.resolve("V3__skill_taxonomy_and_extraction.sql")), "V3 migration must exist");
        assertTrue(Files.exists(migrationDir.resolve("V4__taxonomy_and_constraint_cleanup.sql")), "V4 migration must exist");
        assertTrue(Files.exists(migrationDir.resolve("V5__phase_4_matching_engine.sql")), "V5 migration must exist");

        // Verify NO speculative V6 migration exists
        try (var stream = Files.list(migrationDir)) {
            boolean hasV6 = stream.anyMatch(p -> p.getFileName().toString().startsWith("V6"));
            assertFalse(hasV6, "Phase 4D strictly forbids speculative V6 migrations");
        }

        // Verify Flyway V3 checksum = 11453495 when PostgreSQL connection is available
        String pgUrl = System.getenv().getOrDefault("SPRING_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/fairhire_db");
        try (Connection conn = DriverManager.getConnection(pgUrl, "postgres", "postgres")) {
            var stmt = conn.createStatement();
            var rs = stmt.executeQuery("SELECT checksum FROM flyway_schema_history WHERE version = '3'");
            if (rs.next()) {
                int v3Checksum = rs.getInt("checksum");
                assertEquals(11453495, v3Checksum, "Flyway V3 checksum must remain 11453495");
            }
        } catch (Exception ignored) {
            // Live PostgreSQL not active in local environment, documented truthfully
        }

        // Verify Cascade Deletion integrity: deleting MatchResult cascades to MatchSkillDetails
        Job job = createJob("T32 Cascade Job", "Engineering", List.of(skillJava), Collections.emptyList());
        Resume resume = createResume("T32 Cand", List.of(skillJava), "Java dev");

        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", job.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + resume.getId() + "]}"))
                .andExpect(status().isOk());

        MatchResult mr = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                job.getId(), resume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE
        ).orElseThrow();
        Long matchResultId = mr.getId();

        assertEquals(1, matchSkillDetailRepository.findByMatchResultId(matchResultId).size());

        matchResultRepository.delete(mr);
        matchResultRepository.flush();

        assertTrue(matchResultRepository.findById(matchResultId).isEmpty());
        assertTrue(matchSkillDetailRepository.findByMatchResultId(matchResultId).isEmpty(),
                "Deleting MatchResult must leave zero orphan MatchSkillDetails");
    }
}
