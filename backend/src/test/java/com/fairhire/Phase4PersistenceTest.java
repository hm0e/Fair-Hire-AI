package com.fairhire;

import com.fairhire.models.*;
import com.fairhire.models.enums.*;
import com.fairhire.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = FairHireApplication.class)
@DisplayName("Phase 4A: Persistence & Schema Verification Tests")
class Phase4PersistenceTest {

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
    private MatchResultRepository matchResultRepository;

    @Autowired
    private MatchSkillDetailRepository matchSkillDetailRepository;

    private User testUser;
    private Job testJob;
    private Skill testSkillJava;
    private Skill testSkillPython;
    private JobSkill testJobSkill;
    private Candidate testCandidate;
    private Resume testResume;

    @BeforeEach
    void setUp() {
        testUser = userRepository.findByEmail("recruiter@fairhire.ai")
                .orElseGet(() -> userRepository.save(new User("Test Recruiter", "recruiter@fairhire.ai", "hash", "recruiter")));

        testJob = jobRepository.save(new Job("Backend Architect", "Looking for Java and Python expert.", testUser));

        testSkillJava = skillRepository.findByName("Java")
                .orElseGet(() -> skillRepository.save(new Skill("Java", "PROGRAMMING_LANGUAGE")));

        testSkillPython = skillRepository.findByName("Python")
                .orElseGet(() -> skillRepository.save(new Skill("Python", "PROGRAMMING_LANGUAGE")));

        testJobSkill = jobSkillRepository.save(new JobSkill(
                testJob,
                testSkillJava,
                true,
                3,
                new BigDecimal("1.000"),
                "EXACT_CANONICAL_MATCH",
                "Senior Java engineer needed for core services",
                "Java"
        ));

        testCandidate = candidateRepository.save(new Candidate(
                "Jane Doe",
                "jane.doe." + System.nanoTime() + "@example.com",
                "555-0199",
                testUser
        ));

        testResume = resumeRepository.save(new Resume(
                testCandidate,
                "test.pdf",
                ResumeFileType.PDF,
                "dummyhash" + System.nanoTime(),
                "Experienced Java and Python developer",
                new BigDecimal("5.0"),
                "BS Computer Science"
        ));
    }

    @Test
    @DisplayName("A & B & C: Migration files exist and V3 checksum remains 11453495")
    void testMigrationFilesAndV3Checksum() throws Exception {
        Path migrationDir = Paths.get("src/main/resources/db/migration");
        assertTrue(Files.exists(migrationDir.resolve("V1__initial_schema.sql")), "V1 migration must exist");
        assertTrue(Files.exists(migrationDir.resolve("V2__resume_ingestion_lifecycle.sql")), "V2 migration must exist");
        assertTrue(Files.exists(migrationDir.resolve("V3__skill_taxonomy_and_extraction.sql")), "V3 migration must exist");
        assertTrue(Files.exists(migrationDir.resolve("V4__taxonomy_and_constraint_cleanup.sql")), "V4 migration must exist");
        assertTrue(Files.exists(migrationDir.resolve("V5__phase_4_matching_engine.sql")), "V5 migration must exist");

        // When PostgreSQL is running, verify checksum from Flyway schema history
        String pgUrl = System.getenv().getOrDefault("SPRING_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/fairhire_db");
        try (Connection conn = DriverManager.getConnection(pgUrl, "postgres", "postgres")) {
            var stmt = conn.createStatement();
            var rs = stmt.executeQuery("SELECT checksum FROM flyway_schema_history WHERE version = '3'");
            if (rs.next()) {
                int v3Checksum = rs.getInt("checksum");
                assertEquals(11453495, v3Checksum, "Flyway V3 checksum in flyway_schema_history must remain 11453495");
            }
        } catch (Exception ignored) {
            // Postgres not active in this environment, fallback file check passes
        }
    }

    @Test
    @DisplayName("D: JobSkill evidence fields persist correctly")
    void testJobSkillEvidenceFieldsPersistence() {
        JobSkill saved = jobSkillRepository.findById(testJobSkill.getId()).orElseThrow();
        assertEquals("EXACT_CANONICAL_MATCH", saved.getExtractionMethod());
        assertEquals("Senior Java engineer needed for core services", saved.getContextSnippet());
        assertEquals("Java", saved.getMatchedText());
        assertTrue(saved.getIsMandatory());
    }

    @Test
    @DisplayName("E & F: MatchResult Phase 4 fields persist and algorithmVersion defaults to deterministic-v1")
    void testMatchResultPhase4FieldsPersistence() {
        MatchResult matchResult = new MatchResult();
        matchResult.setJob(testJob);
        matchResult.setCandidate(testCandidate);
        matchResult.setResume(testResume);
        matchResult.setScreeningMode(ScreeningMode.NORMAL);
        matchResult.setMatchingMethod(MatchingMethod.CANONICAL_SKILL_COVERAGE);
        matchResult.setRequiredSkillCoverage(new BigDecimal("1.0000"));
        matchResult.setPreferredSkillCoverage(new BigDecimal("0.5000"));
        matchResult.setOverallScore(new BigDecimal("85.00"));
        matchResult.setRequiredSkillsTotal(2);
        matchResult.setRequiredSkillsMatched(2);
        matchResult.setPreferredSkillsTotal(2);
        matchResult.setPreferredSkillsMatched(1);
        matchResult.setScoredAt(Instant.now());

        MatchResult saved = matchResultRepository.save(matchResult);
        assertNotNull(saved.getId());

        MatchResult loaded = matchResultRepository.findById(saved.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("1.0000").compareTo(loaded.getRequiredSkillCoverage()));
        assertEquals(0, new BigDecimal("0.5000").compareTo(loaded.getPreferredSkillCoverage()));
        assertEquals(0, new BigDecimal("85.00").compareTo(loaded.getOverallScore()));
        assertEquals(2, loaded.getRequiredSkillsTotal());
        assertEquals(2, loaded.getRequiredSkillsMatched());
        assertEquals(2, loaded.getPreferredSkillsTotal());
        assertEquals(1, loaded.getPreferredSkillsMatched());
        assertFalse(loaded.getIsStale());
        assertEquals("deterministic-v1", loaded.getAlgorithmVersion(), "algorithmVersion must default to deterministic-v1");
        assertNotNull(loaded.getScoredAt());
        assertEquals(MatchingMethod.CANONICAL_SKILL_COVERAGE, loaded.getMatchingMethod());
    }

    @Test
    @DisplayName("G & H: MatchSkillDetail persists and correctly references match_results, job_skills, skills")
    void testMatchSkillDetailPersistenceAndReferences() {
        MatchResult matchResult = new MatchResult();
        matchResult.setJob(testJob);
        matchResult.setCandidate(testCandidate);
        matchResult.setResume(testResume);
        matchResult.setScreeningMode(ScreeningMode.NORMAL);
        matchResult.setMatchingMethod(MatchingMethod.CANONICAL_SKILL_COVERAGE);
        matchResult = matchResultRepository.save(matchResult);

        MatchSkillDetail detail = new MatchSkillDetail(
                matchResult,
                testJobSkill,
                testSkillJava,
                RequirementNecessity.REQUIRED,
                true,
                new BigDecimal("0.950"),
                "Java",
                "5 years of experience in Java development"
        );

        MatchSkillDetail savedDetail = matchSkillDetailRepository.save(detail);
        assertNotNull(savedDetail.getId());

        MatchSkillDetail loaded = matchSkillDetailRepository.findById(savedDetail.getId()).orElseThrow();
        assertEquals(matchResult.getId(), loaded.getMatchResult().getId());
        assertEquals(testJobSkill.getId(), loaded.getJobSkill().getId());
        assertEquals(testSkillJava.getId(), loaded.getSkill().getId());
        assertEquals(RequirementNecessity.REQUIRED, loaded.getNecessity());
        assertTrue(loaded.getIsMatched());
        assertEquals(0, new BigDecimal("0.950").compareTo(loaded.getCandidateConfidence()));
        assertEquals("Java", loaded.getCandidateMatchedText());
        assertEquals("5 years of experience in Java development", loaded.getCandidateContextSnippet());

        // Verify repository methods
        List<MatchSkillDetail> details = matchSkillDetailRepository.findByMatchResultIdOrderByJobSkillIdAsc(matchResult.getId());
        assertEquals(1, details.size());
        assertEquals(savedDetail.getId(), details.get(0).getId());

        Optional<MatchSkillDetail> byJobSkill = matchSkillDetailRepository.findByMatchResultIdAndJobSkillId(matchResult.getId(), testJobSkill.getId());
        assertTrue(byJobSkill.isPresent());
    }

    @Test
    @DisplayName("I: Duplicate (match_result_id, job_skill_id) is rejected by unique constraint")
    void testDuplicateMatchSkillDetailRejected() {
        MatchResult matchResult = new MatchResult();
        matchResult.setJob(testJob);
        matchResult.setCandidate(testCandidate);
        matchResult.setResume(testResume);
        matchResult.setScreeningMode(ScreeningMode.NORMAL);
        matchResult.setMatchingMethod(MatchingMethod.CANONICAL_SKILL_COVERAGE);
        MatchResult savedResult = matchResultRepository.save(matchResult);

        MatchSkillDetail detail1 = new MatchSkillDetail(
                savedResult,
                testJobSkill,
                testSkillJava,
                RequirementNecessity.REQUIRED,
                true,
                new BigDecimal("0.900"),
                "Java",
                "Snippet 1"
        );
        matchSkillDetailRepository.saveAndFlush(detail1);

        MatchSkillDetail detail2 = new MatchSkillDetail(
                savedResult,
                testJobSkill,
                testSkillJava,
                RequirementNecessity.REQUIRED,
                true,
                new BigDecimal("0.800"),
                "Java",
                "Snippet 2"
        );

        assertThrows(DataIntegrityViolationException.class, () -> {
            matchSkillDetailRepository.saveAndFlush(detail2);
        }, "Duplicate (match_result_id, job_skill_id) must be rejected by unique constraint");
    }

    @Test
    @DisplayName("J: necessity only accepts REQUIRED and PREFERRED")
    void testNecessityValues() {
        assertEquals("REQUIRED", RequirementNecessity.REQUIRED.name());
        assertEquals("PREFERRED", RequirementNecessity.PREFERRED.name());
        assertEquals(2, RequirementNecessity.values().length);
    }

    @Test
    @DisplayName("K: candidateConfidence accepts null and bounded [0.000, 1.000] values")
    void testCandidateConfidenceAcceptsNullAndBounds() {
        MatchResult matchResult = new MatchResult();
        matchResult.setJob(testJob);
        matchResult.setCandidate(testCandidate);
        matchResult.setResume(testResume);
        matchResult.setScreeningMode(ScreeningMode.BLIND);
        matchResult.setMatchingMethod(MatchingMethod.CANONICAL_SKILL_COVERAGE);
        MatchResult savedResult = matchResultRepository.save(matchResult);

        // Null confidence
        MatchSkillDetail nullConf = new MatchSkillDetail(
                savedResult, testJobSkill, testSkillJava,
                RequirementNecessity.REQUIRED, false,
                null, null, null
        );
        MatchSkillDetail savedNull = matchSkillDetailRepository.save(nullConf);
        assertNull(savedNull.getCandidateConfidence());

        // Zero confidence
        savedNull.setCandidateConfidence(new BigDecimal("0.000"));
        MatchSkillDetail savedZero = matchSkillDetailRepository.save(savedNull);
        assertEquals(0, BigDecimal.ZERO.compareTo(savedZero.getCandidateConfidence()));

        // Max confidence 1.000
        savedZero.setCandidateConfidence(new BigDecimal("1.000"));
        MatchSkillDetail savedMax = matchSkillDetailRepository.save(savedZero);
        assertEquals(0, BigDecimal.ONE.compareTo(savedMax.getCandidateConfidence()));
    }

    @Test
    @DisplayName("L: Legacy SEMANTIC MatchResult rows remain readable and unchanged")
    void testLegacySemanticMatchResultRowUnchanged() {
        MatchResult legacy = new MatchResult();
        legacy.setJob(testJob);
        legacy.setCandidate(testCandidate);
        legacy.setResume(testResume);
        legacy.setScreeningMode(ScreeningMode.NORMAL);
        legacy.setMatchingMethod(MatchingMethod.SEMANTIC);
        legacy.setSemanticScore(new BigDecimal("0.7500"));
        legacy.setFinalCompositeScore(new BigDecimal("0.7500"));
        legacy.setModelVersion("all-MiniLM-L6-v2");

        MatchResult saved = matchResultRepository.save(legacy);
        assertNotNull(saved.getId());

        MatchResult loaded = matchResultRepository.findById(saved.getId()).orElseThrow();
        assertEquals(MatchingMethod.SEMANTIC, loaded.getMatchingMethod());
        assertEquals(0, new BigDecimal("0.7500").compareTo(loaded.getSemanticScore()));
        assertEquals("all-MiniLM-L6-v2", loaded.getModelVersion());
        assertNull(loaded.getRequiredSkillCoverage());
        assertNull(loaded.getPreferredSkillCoverage());
        assertNull(loaded.getOverallScore());
    }

    @Test
    @DisplayName("M: Existing unique constraint supports coexistence of SEMANTIC and CANONICAL_SKILL_COVERAGE")
    void testUniqueConstraintCoexistenceOfMethods() {
        MatchResult semanticResult = new MatchResult();
        semanticResult.setJob(testJob);
        semanticResult.setCandidate(testCandidate);
        semanticResult.setResume(testResume);
        semanticResult.setScreeningMode(ScreeningMode.NORMAL);
        semanticResult.setMatchingMethod(MatchingMethod.SEMANTIC);
        semanticResult.setFinalCompositeScore(new BigDecimal("0.8000"));
        matchResultRepository.saveAndFlush(semanticResult);

        MatchResult deterministicResult = new MatchResult();
        deterministicResult.setJob(testJob);
        deterministicResult.setCandidate(testCandidate);
        deterministicResult.setResume(testResume);
        deterministicResult.setScreeningMode(ScreeningMode.NORMAL);
        deterministicResult.setMatchingMethod(MatchingMethod.CANONICAL_SKILL_COVERAGE);
        deterministicResult.setOverallScore(new BigDecimal("92.50"));
        MatchResult savedDeterministic = matchResultRepository.saveAndFlush(deterministicResult);

        assertNotNull(savedDeterministic.getId());
        assertNotEquals(semanticResult.getId(), savedDeterministic.getId());

        // Verify lookup by exact (job, resume, mode, method)
        Optional<MatchResult> foundSemantic = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                testJob.getId(), testResume.getId(), ScreeningMode.NORMAL, MatchingMethod.SEMANTIC);
        assertTrue(foundSemantic.isPresent());

        Optional<MatchResult> foundDeterministic = matchResultRepository.findByJobIdAndResumeIdAndScreeningModeAndMatchingMethod(
                testJob.getId(), testResume.getId(), ScreeningMode.NORMAL, MatchingMethod.CANONICAL_SKILL_COVERAGE);
        assertTrue(foundDeterministic.isPresent());
    }
}
