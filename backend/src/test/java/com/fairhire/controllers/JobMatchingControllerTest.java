package com.fairhire.controllers;

import com.fairhire.FairHireApplication;
import com.fairhire.config.JwtUtils;
import com.fairhire.models.*;
import com.fairhire.models.enums.*;
import com.fairhire.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = FairHireApplication.class)
@AutoConfigureMockMvc
@DisplayName("Phase 4B: Job Matching Controller REST API Tests")
class JobMatchingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

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

    private User testRecruiter;
    private Skill skillJava;
    private Skill skillSpring;
    private Job testJob;
    private Resume testResume;
    private String token;
    private String noDeptToken;

    private MockHttpServletRequestBuilder authPost(String url, Object... uriVars) {
        return post(url, uriVars).header("Authorization", "Bearer " + token);
    }

    private MockHttpServletRequestBuilder authGet(String url, Object... uriVars) {
        return get(url, uriVars).header("Authorization", "Bearer " + token);
    }

    @BeforeEach
    void setUp() {
        testRecruiter = userRepository.findByEmail("recruiter.ctrl@fairhire.ai")
                .orElseGet(() -> userRepository.save(new User("Ctrl Recruiter", "recruiter.ctrl@fairhire.ai", "hash", UserRole.RECRUITER)));

        token = jwtUtils.generateToken(testRecruiter.getId(), testRecruiter.getEmail(), "RECRUITER", "Engineering");
        noDeptToken = jwtUtils.generateToken(testRecruiter.getId(), testRecruiter.getEmail(), "RECRUITER");

        skillJava = skillRepository.findByName("Java")
                .orElseGet(() -> skillRepository.save(new Skill("Java", "PROGRAMMING_LANGUAGE")));

        skillSpring = skillRepository.findByName("Spring Boot")
                .orElseGet(() -> skillRepository.save(new Skill("Spring Boot", "FRAMEWORK")));

        testJob = jobRepository.save(new Job("API Test Engineer", "Description", testRecruiter));
        testJob.setDepartment("Engineering");
        testJob = jobRepository.save(testJob);

        jobSkillRepository.save(new JobSkill(testJob, skillJava, true, 2, new BigDecimal("1.000"), "MANUAL", "Java", "Java"));
        jobSkillRepository.save(new JobSkill(testJob, skillSpring, false, 1, new BigDecimal("1.000"), "MANUAL", "Spring", "Spring Boot"));

        Candidate candidate = candidateRepository.save(new Candidate("API Candidate", "api.cand." + System.nanoTime() + "@example.com", "555-0101", testRecruiter));
        testResume = resumeRepository.save(new Resume(candidate, "resume.pdf", ResumeFileType.PDF, "hash_" + System.nanoTime(), "Resume text", new BigDecimal("3.0"), "BS"));
        testResume.setParsingStatus(ParsingStatus.READY);
        testResume = resumeRepository.save(testResume);

        resumeSkillRepository.save(new ResumeSkill(testResume, skillJava, new BigDecimal("0.900"), "Java developer", "Java", "EXACT_CANONICAL_MATCH"));
    }

    @Test
    @DisplayName("POST /api/v1/jobs/{jobId}/matches - Success 200 OK for authorized caller")
    void testPostMatchesSuccess() throws Exception {
        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", testJob.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + testResume.getId() + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job_id", is(testJob.getId().intValue())))
                .andExpect(jsonPath("$.algorithm_version", is("deterministic-v1")))
                .andExpect(jsonPath("$.matching_method", is("CANONICAL_SKILL_COVERAGE")))
                .andExpect(jsonPath("$.total_evaluated", is(1)))
                .andExpect(jsonPath("$.results[0].overall_score", is(80.0)))
                .andExpect(jsonPath("$.results[0].required_skills_matched", is(1)))
                .andExpect(jsonPath("$.results[0].required_skills_total", is(1)))
                .andExpect(jsonPath("$.results[0].preferred_skills_matched", is(0)))
                .andExpect(jsonPath("$.results[0].preferred_skills_total", is(1)));
    }

    @Test
    @DisplayName("POST /api/jobs/{jobId}/matches (alias) - Success 200 OK for authorized caller")
    void testPostMatchesAliasSuccess() throws Exception {
        mockMvc.perform(authPost("/api/jobs/{jobId}/matches", testJob.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + testResume.getId() + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm_version", is("deterministic-v1")));
    }

    @Test
    @DisplayName("POST /api/v1/jobs/{jobId}/matches - 404 when Job Not Found")
    void testPostMatchesJobNotFound() throws Exception {
        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", 999999L)
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("POST /api/v1/jobs/{jobId}/matches - 422 MATCHING_REQUIREMENTS_NOT_FOUND when Job has 0 requirements")
    void testPostMatchesZeroRequirements() throws Exception {
        Job emptyJob = jobRepository.save(new Job("Empty Requirements Job", "Desc", testRecruiter));
        emptyJob.setDepartment("Engineering");
        emptyJob = jobRepository.save(emptyJob);

        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", emptyJob.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + testResume.getId() + "]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status", is(422)))
                .andExpect(jsonPath("$.error", is("MATCHING_REQUIREMENTS_NOT_FOUND")));
    }

    @Test
    @DisplayName("POST /api/v1/jobs/{jobId}/matches - 422 BATCH_SIZE_LIMIT_EXCEEDED when resumes > 100")
    void testPostMatchesBatchLimitExceeded() throws Exception {
        StringBuilder sb = new StringBuilder("{\"resume_ids\": [");
        for (int i = 1; i <= 101; i++) {
            sb.append(i);
            if (i < 101) sb.append(", ");
        }
        sb.append("]}");

        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", testJob.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sb.toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status", is(422)))
                .andExpect(jsonPath("$.error", is("BATCH_SIZE_LIMIT_EXCEEDED")));
    }

    @Test
    @DisplayName("POST /api/v1/jobs/{jobId}/matches - 403 UNAUTHORIZED_RESOURCE_ACCESS when caller department is missing")
    void testPostMatchesMissingDepartment() throws Exception {
        mockMvc.perform(post("/api/v1/jobs/{jobId}/matches", testJob.getId())
                        .header("Authorization", "Bearer " + noDeptToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + testResume.getId() + "]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED_RESOURCE_ACCESS")));
    }

    @Test
    @DisplayName("POST /api/v1/jobs/{jobId}/matches - 403 UNAUTHORIZED_RESOURCE_ACCESS when X-Department is blank")
    void testPostMatchesBlankDepartment() throws Exception {
        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", testJob.getId())
                        .header("X-Department", "   ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + testResume.getId() + "]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED_RESOURCE_ACCESS")));
    }

    @Test
    @DisplayName("POST /api/v1/jobs/{jobId}/matches - 403 UNAUTHORIZED_RESOURCE_ACCESS on cross-department request")
    void testPostMatchesUnauthorizedDepartment() throws Exception {
        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", testJob.getId())
                        .header("X-Department", "Marketing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + testResume.getId() + "]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED_RESOURCE_ACCESS")));
    }

    @Test
    @DisplayName("POST /api/v1/jobs/{jobId}/matches - 400 INVALID_SCREENING_MODE on invalid mode")
    void testPostMatchesInvalidScreeningMode() throws Exception {
        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", testJob.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + testResume.getId() + "], \"screening_mode\": \"INVALID_MODE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("INVALID_SCREENING_MODE")));
    }

    @Test
    @DisplayName("POST /api/v1/jobs/{jobId}/matches - 422 INVALID_MATCHING_METHOD on unapproved method")
    void testPostMatchesInvalidMatchingMethod() throws Exception {
        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", testJob.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + testResume.getId() + "], \"matching_method\": \"SEMANTIC\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status", is(422)))
                .andExpect(jsonPath("$.error", is("INVALID_MATCHING_METHOD")));
    }

    @Test
    @DisplayName("GET /api/v1/jobs/{jobId}/matches - 200 OK returns matches list for authorized caller")
    void testGetMatchesList() throws Exception {
        // Run match first
        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", testJob.getId())
                .header("X-Department", "Engineering")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resume_ids\": [" + testResume.getId() + "]}"));

        mockMvc.perform(authGet("/api/v1/jobs/{jobId}/matches", testJob.getId())
                        .header("X-Department", "Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job_id", is(testJob.getId().intValue())))
                .andExpect(jsonPath("$.total_evaluated", is(1)))
                .andExpect(jsonPath("$.results[0].overall_score", is(80.0)));
    }

    @Test
    @DisplayName("GET /api/v1/jobs/{jobId}/matches - 403 FORBIDDEN when caller department is missing")
    void testGetMatchesListMissingDepartment() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/{jobId}/matches", testJob.getId())
                        .header("Authorization", "Bearer " + noDeptToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED_RESOURCE_ACCESS")));
    }

    @Test
    @DisplayName("GET /api/v1/jobs/{jobId}/matches - 403 FORBIDDEN on wrong department")
    void testGetMatchesListUnauthorizedDepartment() throws Exception {
        mockMvc.perform(authGet("/api/v1/jobs/{jobId}/matches", testJob.getId())
                        .header("X-Department", "Marketing"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED_RESOURCE_ACCESS")));
    }

    @Test
    @DisplayName("GET /api/v1/jobs/{jobId}/matches/{resumeId} - 200 OK returns detailed decomposed match with explainability metadata")
    void testGetMatchDetail() throws Exception {
        // Run match first
        mockMvc.perform(authPost("/api/v1/jobs/{jobId}/matches", testJob.getId())
                .header("X-Department", "Engineering")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resume_ids\": [" + testResume.getId() + "]}"));

        mockMvc.perform(authGet("/api/v1/jobs/{jobId}/matches/{resumeId}", testJob.getId(), testResume.getId())
                        .header("X-Department", "Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job_id", is(testJob.getId().intValue())))
                .andExpect(jsonPath("$.resume_id", is(testResume.getId().intValue())))
                .andExpect(jsonPath("$.algorithm_version", is("deterministic-v1")))
                .andExpect(jsonPath("$.matching_method", is("CANONICAL_SKILL_COVERAGE")))
                .andExpect(jsonPath("$.overall_score", is(80.0)))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.is_stale", is(false)))
                .andExpect(jsonPath("$.required_weight", is(0.80)))
                .andExpect(jsonPath("$.preferred_weight", is(0.20)))
                .andExpect(jsonPath("$.required_contribution", is(80.00)))
                .andExpect(jsonPath("$.preferred_contribution", is(0.00)))
                .andExpect(jsonPath("$.required_skills", hasSize(1)))
                .andExpect(jsonPath("$.required_skills[0].skill_name", is("Java")))
                .andExpect(jsonPath("$.required_skills[0].is_matched", is(true)))
                .andExpect(jsonPath("$.required_skills[0].matched", is(true)))
                .andExpect(jsonPath("$.preferred_skills", hasSize(1)))
                .andExpect(jsonPath("$.preferred_skills[0].skill_name", is("Spring Boot")))
                .andExpect(jsonPath("$.preferred_skills[0].is_matched", is(false)))
                .andExpect(jsonPath("$.preferred_skills[0].matched", is(false)));
    }

    @Test
    @DisplayName("GET /api/v1/jobs/{jobId}/matches/{resumeId} - 403 UNAUTHORIZED_RESOURCE_ACCESS when caller department is missing")
    void testGetMatchDetailMissingDepartment() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/{jobId}/matches/{resumeId}", testJob.getId(), testResume.getId())
                        .header("Authorization", "Bearer " + noDeptToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED_RESOURCE_ACCESS")));
    }

    @Test
    @DisplayName("GET /api/v1/jobs/{jobId}/matches/{resumeId} - 403 UNAUTHORIZED_RESOURCE_ACCESS on blank X-Department")
    void testGetMatchDetailBlankDepartment() throws Exception {
        mockMvc.perform(authGet("/api/v1/jobs/{jobId}/matches/{resumeId}", testJob.getId(), testResume.getId())
                        .header("X-Department", "   "))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED_RESOURCE_ACCESS")));
    }

    @Test
    @DisplayName("GET /api/v1/jobs/{jobId}/matches/{resumeId} - 403 UNAUTHORIZED_RESOURCE_ACCESS on cross-department request")
    void testGetMatchDetailUnauthorizedDepartment() throws Exception {
        mockMvc.perform(authGet("/api/v1/jobs/{jobId}/matches/{resumeId}", testJob.getId(), testResume.getId())
                        .header("X-Department", "Marketing"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED_RESOURCE_ACCESS")));
    }

    @Test
    @DisplayName("GET /api/v1/jobs/{jobId}/matches/{resumeId} - 404 when match not found for authorized caller")
    void testGetMatchDetailNotFound() throws Exception {
        mockMvc.perform(authGet("/api/v1/jobs/{jobId}/matches/{resumeId}", testJob.getId(), 999999L)
                        .header("X-Department", "Engineering"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("RESOURCE_NOT_FOUND")));
    }
}
