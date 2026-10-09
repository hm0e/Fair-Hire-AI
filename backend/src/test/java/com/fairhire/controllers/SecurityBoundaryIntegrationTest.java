package com.fairhire.controllers;

import com.fairhire.FairHireApplication;
import com.fairhire.config.JwtUtils;
import com.fairhire.models.*;
import com.fairhire.models.enums.*;
import com.fairhire.repositories.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = FairHireApplication.class)
@AutoConfigureMockMvc
@DisplayName("Security Boundary & Trusted Identity Integration Tests (S01 - S16)")
class SecurityBoundaryIntegrationTest {

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

    private User recruiter;
    private Job engineeringJob;
    private Job marketingJob;
    private Resume testResume;
    private String engineeringToken;
    private String marketingToken;
    private String noDeptToken;

    @BeforeEach
    void setUp() {
        recruiter = userRepository.findByEmail("sec.recruiter@fairhire.test")
                .orElseGet(() -> userRepository.save(new User("Sec Recruiter", "sec.recruiter@fairhire.test", "hashed_pw", UserRole.RECRUITER)));

        engineeringToken = jwtUtils.generateToken(recruiter.getId(), recruiter.getEmail(), "RECRUITER", "Engineering");
        marketingToken = jwtUtils.generateToken(recruiter.getId(), recruiter.getEmail(), "RECRUITER", "Marketing");
        noDeptToken = jwtUtils.generateToken(recruiter.getId(), recruiter.getEmail(), "RECRUITER", null);

        Skill skillJava = skillRepository.findByName("Java")
                .orElseGet(() -> skillRepository.save(new Skill("Java", "PROGRAMMING_LANGUAGE")));

        engineeringJob = jobRepository.save(new Job("Backend Engineer", "Java Spring engineer", recruiter));
        engineeringJob.setDepartment("Engineering");
        engineeringJob = jobRepository.save(engineeringJob);

        marketingJob = jobRepository.save(new Job("Marketing Specialist", "Digital marketing role", recruiter));
        marketingJob.setDepartment("Marketing");
        marketingJob = jobRepository.save(marketingJob);

        jobSkillRepository.save(new JobSkill(engineeringJob, skillJava, true, 2, new BigDecimal("1.000"), "MANUAL", "Java", "Java"));
        jobSkillRepository.save(new JobSkill(marketingJob, skillJava, true, 1, new BigDecimal("1.000"), "MANUAL", "Java", "Java"));

        Candidate candidate = candidateRepository.save(new Candidate("Sec Candidate", "cand." + System.nanoTime() + "@fairhire.test", "555-0199", recruiter));

        testResume = resumeRepository.save(new Resume(candidate, "resume.pdf", ResumeFileType.PDF, "hash_" + System.nanoTime(), "Java experience", new BigDecimal("3.0"), "BS"));
        testResume.setParsingStatus(ParsingStatus.READY);
        testResume = resumeRepository.save(testResume);

        resumeSkillRepository.save(new ResumeSkill(testResume, skillJava, new BigDecimal("0.950"), "Java experience", "Java", "EXACT_CANONICAL_MATCH"));
    }

    @Test
    @DisplayName("S01: anonymous POST matching is rejected with 401 (missing Authorization header)")
    void testS01_AnonymousPostMatching() throws Exception {
        mockMvc.perform(post("/api/v1/jobs/{jobId}/matches", engineeringJob.getId())
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + testResume.getId() + "]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("S02: anonymous GET match list is rejected with 401 (missing Authorization header)")
    void testS02_AnonymousGetMatchList() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/{jobId}/matches", engineeringJob.getId())
                        .header("X-Department", "Engineering"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("S03: anonymous GET match detail is rejected with 401 (missing Authorization header)")
    void testS03_AnonymousGetMatchDetail() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/{jobId}/matches/{resumeId}", engineeringJob.getId(), testResume.getId())
                        .header("X-Department", "Engineering"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("S04: blank or whitespace Authorization header is rejected with 401 at authentication boundary")
    void testS04_BlankAuthorizationHeader_RejectedWith401() throws Exception {
        // Empty Authorization header
        mockMvc.perform(post("/api/v1/jobs/{jobId}/matches", engineeringJob.getId())
                        .header("Authorization", "")
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + testResume.getId() + "]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));

        // Whitespace Authorization header
        mockMvc.perform(post("/api/v1/jobs/{jobId}/matches", engineeringJob.getId())
                        .header("Authorization", "   ")
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + testResume.getId() + "]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));

        // Empty Bearer prefix with no token
        mockMvc.perform(post("/api/v1/jobs/{jobId}/matches", engineeringJob.getId())
                        .header("Authorization", "Bearer ")
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + testResume.getId() + "]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("S05: valid Engineering user + Engineering job succeeds with 200")
    void testS05_EngineeringUser_EngineeringJob_Allowed() throws Exception {
        mockMvc.perform(post("/api/v1/jobs/{jobId}/matches", engineeringJob.getId())
                        .header("Authorization", "Bearer " + engineeringToken)
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + testResume.getId() + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job_id", is(engineeringJob.getId().intValue())));
    }

    @Test
    @DisplayName("S06: valid Engineering user + Marketing job is rejected with 403")
    void testS06_EngineeringUser_MarketingJob_Forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/jobs/{jobId}/matches", marketingJob.getId())
                        .header("Authorization", "Bearer " + engineeringToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + testResume.getId() + "]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED_RESOURCE_ACCESS")));
    }

    @Test
    @DisplayName("S07: valid Engineering user + X-Department: Marketing is rejected with 403")
    void testS07_EngineeringUser_XDepartmentMarketing_Forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/jobs/{jobId}/matches", engineeringJob.getId())
                        .header("Authorization", "Bearer " + engineeringToken)
                        .header("X-Department", "Marketing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + testResume.getId() + "]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED_RESOURCE_ACCESS")))
                .andExpect(jsonPath("$.message", containsString("cannot override trusted identity")));
    }

    @Test
    @DisplayName("S08: valid Engineering user + X-Department: Engineering is allowed (200)")
    void testS08_EngineeringUser_XDepartmentEngineering_Allowed() throws Exception {
        mockMvc.perform(post("/api/v1/jobs/{jobId}/matches", engineeringJob.getId())
                        .header("Authorization", "Bearer " + engineeringToken)
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + testResume.getId() + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job_id", is(engineeringJob.getId().intValue())));
    }

    @Test
    @DisplayName("S09: valid Engineering user with no X-Department header succeeds (trusted-identity behavior)")
    void testS09_EngineeringUser_NoXDepartment_Allowed() throws Exception {
        mockMvc.perform(post("/api/v1/jobs/{jobId}/matches", engineeringJob.getId())
                        .header("Authorization", "Bearer " + engineeringToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + testResume.getId() + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job_id", is(engineeringJob.getId().intValue())));
    }

    @Test
    @DisplayName("S10: changing X-Department cannot change authorization identity (spoofing attempt rejected with 403)")
    void testS10_ChangingXDepartmentCannotChangeAuthorizationIdentity() throws Exception {
        // Engineering user attempts to spoof X-Department: Marketing to access a Marketing job
        mockMvc.perform(post("/api/v1/jobs/{jobId}/matches", marketingJob.getId())
                        .header("Authorization", "Bearer " + engineeringToken)
                        .header("X-Department", "Marketing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + testResume.getId() + "]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED_RESOURCE_ACCESS")));
    }

    @Test
    @DisplayName("S11: valid Engineering user + Blank X-Department header is rejected with 403")
    void testS11_EngineeringUser_BlankXDepartment_Forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/jobs/{jobId}/matches", engineeringJob.getId())
                        .header("Authorization", "Bearer " + engineeringToken)
                        .header("X-Department", "   ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + testResume.getId() + "]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED_RESOURCE_ACCESS")));
    }

    @Test
    @DisplayName("S12: tampered JWT signature is rejected with 401")
    void testS12_InvalidJwtSignature() throws Exception {
        String badKey = "different-secret-key-32chars-for-forgery-test!!";
        String forgedToken = Jwts.builder()
                .subject(String.valueOf(recruiter.getId()))
                .claims(Map.of("email", recruiter.getEmail(), "role", "RECRUITER", "department", "Engineering"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(Keys.hmacShaKeyFor(badKey.getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(post("/api/v1/jobs/{jobId}/matches", engineeringJob.getId())
                        .header("Authorization", "Bearer " + forgedToken)
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + testResume.getId() + "]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("S13: expired JWT is rejected with 401")
    void testS13_ExpiredJwt() throws Exception {
        String expiredToken = jwtUtils.generateExpiredToken(recruiter.getId(), recruiter.getEmail(), "RECRUITER", "Engineering");

        mockMvc.perform(post("/api/v1/jobs/{jobId}/matches", engineeringJob.getId())
                        .header("Authorization", "Bearer " + expiredToken)
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + testResume.getId() + "]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("S14: malformed JWT is rejected with 401")
    void testS14_MalformedJwt() throws Exception {
        mockMvc.perform(post("/api/v1/jobs/{jobId}/matches", engineeringJob.getId())
                        .header("Authorization", "Bearer not.a.valid.jwt.token.structure")
                        .header("X-Department", "Engineering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + testResume.getId() + "]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("S15: cross-department IDOR attempt is rejected with 403 before candidate data access")
    void testS15_CrossDepartmentIdor() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/{jobId}/matches", marketingJob.getId())
                        .header("Authorization", "Bearer " + engineeringToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED_RESOURCE_ACCESS")));
    }

    @Test
    @DisplayName("S16: authenticated user with no department claim fails closed with 403")
    void testS16_UserWithNoDepartmentClaim_FailClosed() throws Exception {
        mockMvc.perform(post("/api/v1/jobs/{jobId}/matches", engineeringJob.getId())
                        .header("Authorization", "Bearer " + noDeptToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resume_ids\": [" + testResume.getId() + "]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED_RESOURCE_ACCESS")));
    }
}
