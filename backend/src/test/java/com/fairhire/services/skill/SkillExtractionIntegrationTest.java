package com.fairhire.services.skill;

import com.fairhire.FairHireApplication;
import com.fairhire.dto.ExtractedSkillDto;
import com.fairhire.dto.ResumeUploadResponse;
import com.fairhire.models.Resume;
import com.fairhire.models.ResumeSkill;
import com.fairhire.models.Skill;
import com.fairhire.models.enums.ParsingStatus;
import com.fairhire.models.enums.ResumeFileType;
import com.fairhire.repositories.ResumeRepository;
import com.fairhire.repositories.ResumeSkillRepository;
import com.fairhire.repositories.SkillRepository;
import com.fairhire.services.ResumeService;
import com.fairhire.util.TestDocumentGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = FairHireApplication.class)
@DisplayName("Phase 3B: Skill Extraction Integration Tests")
class SkillExtractionIntegrationTest {

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private SkillExtractionService skillExtractionService;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private ResumeSkillRepository resumeSkillRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private ResumeStatusUpdater resumeStatusUpdater;

    @BeforeEach
    void ensureTaxonomySeeded() {
        // Ensure baseline canonical skills are present in testing database
        seedOrUpdateSkill("Java", "PROGRAMMING_LANGUAGE", List.of("Java SE", "Core Java", "Java 17"));
        seedOrUpdateSkill("Python", "PROGRAMMING_LANGUAGE", List.of("Python 3", "Python3"));
        seedOrUpdateSkill("Spring Boot", "FRAMEWORK", List.of("SpringBoot", "Spring Boot Framework"));
        seedOrUpdateSkill("React", "FRAMEWORK", List.of("React.js", "ReactJS"));
        seedOrUpdateSkill("Docker", "CLOUD_DEVOPS", List.of("Docker containers", "Docker Compose"));
        seedOrUpdateSkill("PostgreSQL", "DATABASE", List.of("Postgres", "PostgreSQL DB"));
        seedOrUpdateSkill("JavaScript", "PROGRAMMING_LANGUAGE", List.of("JS", "ECMAScript"));
        seedOrUpdateSkill("Kubernetes", "CLOUD_DEVOPS", List.of("K8s", "Kube"));
    }

    private void seedOrUpdateSkill(String name, String category, List<String> synonyms) {
        Skill skill = skillRepository.findByName(name).orElseGet(() -> new Skill(name, category, synonyms, true));
        skill.setCategory(category);
        skill.setSynonyms(synonyms);
        skill.setIsActive(true);
        skillRepository.save(skill);
    }

    @Test
    @Transactional
    @DisplayName("End-to-End: Resume upload extracts canonical skills, persists ResumeSkills, and sets status to READY")
    void testEndToEndSkillExtractionOnUpload() throws IOException {
        String resumeContent = """
                Diana Prince
                Senior Backend Software Engineer
                Summary:
                Experienced backend developer with strong expertise in Java, Spring Boot, and PostgreSQL.
                Built scalable containerized microservices deployed using Docker and Kubernetes.
                Skills:
                Java, Python, SpringBoot, Docker, Postgres
                """;

        byte[] pdfBytes = TestDocumentGenerator.createValidPdf(List.of(resumeContent));
        MockMultipartFile file = new MockMultipartFile(
                "file", "diana_resume.pdf", "application/pdf", pdfBytes
        );

        ResumeUploadResponse response = resumeService.ingestResume(file, null, "Diana Prince", "diana@example.com");

        assertNotNull(response.resumeId());
        assertEquals(ParsingStatus.READY, response.parsingStatus(),
                "Resume should transition to READY after successful skill extraction");

        // Verify extracted skills in response DTO
        assertThat(response.skillsCount()).isGreaterThanOrEqualTo(4);
        List<String> extractedNames = response.extractedSkills().stream().map(ExtractedSkillDto::skillName).toList();
        assertThat(extractedNames).contains("Java", "Spring Boot", "PostgreSQL", "Docker");

        // Verify database persistence in resume_skills table
        List<ResumeSkill> persistedSkills = resumeSkillRepository.findByResumeId(response.resumeId());
        assertThat(persistedSkills).isNotEmpty();

        for (ResumeSkill rs : persistedSkills) {
            assertNotNull(rs.getId());
            assertEquals(response.resumeId(), rs.getResume().getId());
            assertNotNull(rs.getSkill());
            assertNotNull(rs.getExtractionConfidence());
            assertTrue(rs.getExtractionConfidence().compareTo(BigDecimal.ZERO) > 0);
            assertNotNull(rs.getContextSnippet());
            assertFalse(rs.getContextSnippet().isBlank());
            assertNotNull(rs.getExtractionMethod());
            assertNotNull(rs.getCreatedAt());
        }

        // Verify alias match metadata
        ResumeSkill springBootSkill = persistedSkills.stream()
                .filter(rs -> rs.getSkill().getName().equals("Spring Boot"))
                .findFirst()
                .orElseThrow();
        assertThat(springBootSkill.getExtractionConfidence()).isGreaterThan(new BigDecimal("0.900"));
        assertThat(springBootSkill.getContextSnippet()).isNotEmpty();
    }

    @Test
    @DisplayName("Idempotency: Re-running skill extraction does not violate uniqueness or create duplicate rows")
    void testSkillExtractionIdempotency() {
        String rawText = "Experienced in Python, Docker, and PostgreSQL databases.";
        ResumeUploadResponse initial = resumeService.ingestRawText(rawText, "Evan Wright", "evan@example.com");

        Long resumeId = initial.resumeId();
        Resume resume = resumeRepository.findById(resumeId).orElseThrow();

        List<ResumeSkill> firstRun = resumeSkillRepository.findByResumeId(resumeId);
        int initialCount = firstRun.size();
        assertThat(initialCount).isGreaterThanOrEqualTo(3);

        // Re-run extraction explicitly
        List<ExtractedSkillDto> secondRun = skillExtractionService.extractAndSaveSkills(resume);

        List<ResumeSkill> afterSecondRun = resumeSkillRepository.findByResumeId(resumeId);
        assertEquals(initialCount, afterSecondRun.size(),
                "Re-running skill extraction must replace previous records without duplicating rows");
        assertEquals(initialCount, secondRun.size());
    }

    @Test
    @DisplayName("Taxonomy & Alias Resolution: Resolves diverse synonyms to single canonical representation")
    void testTaxonomyAliasResolution() {
        String text = "Expertise with K8s cluster administration, Postgres databases, and Python 3 scripting.";
        ResumeUploadResponse response = resumeService.ingestRawText(text, "Fiona Gallagher", "fiona@example.com");

        List<ExtractedSkillDto> skills = skillExtractionService.getSkillsForResume(response.resumeId());
        List<String> names = skills.stream().map(ExtractedSkillDto::skillName).toList();

        assertThat(names).contains("Kubernetes", "PostgreSQL", "Python");
        assertThat(names).doesNotContain("K8s", "Postgres", "Python 3");
    }

    @Test
    @DisplayName("Query Service: Successfully fetches extracted skills for a given resume")
    void testGetSkillsForResume() {
        String text = "Tech Stack: Java, React, Docker.";
        ResumeUploadResponse response = resumeService.ingestRawText(text, "George Clark", "george@example.com");

        List<ExtractedSkillDto> skills = skillExtractionService.getSkillsForResume(response.resumeId());
        assertThat(skills).hasSize(3);

        ExtractedSkillDto javaSkill = skills.stream().filter(s -> s.skillName().equals("Java")).findFirst().orElseThrow();
        assertEquals("PROGRAMMING_LANGUAGE", javaSkill.category());
        assertEquals("Java", javaSkill.matchedText());
        assertEquals("EXACT_CANONICAL_MATCH", javaSkill.extractionMethod());
    }

    // ========================================================================
    // B-05 Regression Test: Non-existent Resume ID 404 Validation
    // ========================================================================

    @Test
    @DisplayName("B-05: Non-existent resume ID throws IllegalArgumentException on getSkillsForResume")
    void testGetSkillsForNonExistentResumeThrowsNotFound() {
        Long nonExistentId = 999999L;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                skillExtractionService.getSkillsForResume(nonExistentId)
        );
        assertThat(ex.getMessage()).contains("not found");
    }

    // ========================================================================
    // B-06 Regression Test: Blank Resume Lifecycle Failures
    // ========================================================================

    @Test
    @DisplayName("B-06: Blank raw text resume fails skill extraction and transitions to FAILED (never READY)")
    void testBlankResumeFailsSkillExtraction() {
        Resume resume = new Resume();
        resume.setCandidate(resumeRepository.findAll().get(0).getCandidate());
        resume.setFileName("blank.txt");
        resume.setFileType(ResumeFileType.TXT);
        resume.setFileHashSha256("sha256blankdummy" + System.currentTimeMillis());
        resume.setRawText("   ");
        resume.setParsingStatus(ParsingStatus.PARSED);
        resume = resumeRepository.save(resume);

        List<ExtractedSkillDto> skills = skillExtractionService.extractAndSaveSkills(resume);
        assertThat(skills).isEmpty();

        Resume updated = resumeRepository.findById(resume.getId()).orElseThrow();
        assertEquals(ParsingStatus.FAILED, updated.getParsingStatus());
        assertThat(updated.getParsingError()).contains("Cannot extract skills");
    }

    // ========================================================================
    // B-07 Regression Test: Concurrent Extraction Serialization
    // ========================================================================

    @Test
    @DisplayName("B-07: Concurrent re-extraction requests on the SAME resume serialize safely without StaleObjectStateException")
    void testConcurrentReExtractionSerialization() throws Exception {
        String text = "Senior Engineer with Java, Spring Boot, Docker, and PostgreSQL.";
        ResumeUploadResponse uploaded = resumeService.ingestRawText(text, "Concurrency Candidate", "concur." + System.currentTimeMillis() + "@example.com");
        Long resumeId = uploaded.resumeId();

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    List<ExtractedSkillDto> skills = skillExtractionService.extractSkillsForResumeId(resumeId);
                    if (skills != null && !skills.isEmpty()) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception ex) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All concurrent tasks should complete within timeout");
        assertThat(errorCount.get()).isEqualTo(0);
        assertThat(successCount.get()).isEqualTo(threadCount);

        List<ResumeSkill> skills = resumeSkillRepository.findByResumeId(resumeId);
        assertThat(skills).isNotEmpty();
    }

    // ========================================================================
    // B-08 Regression Test: Failure Status Persistence (REQUIRES_NEW)
    // ========================================================================

    @Test
    @DisplayName("B-08: Transaction rollback on failure persists SKILL_EXTRACTION_FAILED status and error message")
    void testSkillExtractionFailureStatusPersisted() {
        Resume resume = new Resume();
        resume.setCandidate(resumeRepository.findAll().get(0).getCandidate());
        resume.setFileName("failing.txt");
        resume.setFileType(ResumeFileType.TXT);
        resume.setFileHashSha256("sha256faildummy" + System.currentTimeMillis());
        resume.setRawText("Expert in Java and Docker.");
        resume.setParsingStatus(ParsingStatus.PARSED);
        resume = resumeRepository.save(resume);

        resumeStatusUpdater.markSkillExtractionFailed(resume.getId(), "Skill extraction failed: simulated failure");

        Resume updated = resumeRepository.findById(resume.getId()).orElseThrow();
        assertEquals(ParsingStatus.SKILL_EXTRACTION_FAILED, updated.getParsingStatus());
        assertThat(updated.getParsingError()).contains("simulated failure");
    }
}
