package com.fairhire.controllers;

import com.fairhire.FairHireApplication;
import com.fairhire.dto.ResumeUploadResponse;
import com.fairhire.models.Resume;
import com.fairhire.models.enums.ParsingStatus;
import com.fairhire.models.enums.ResumeFileType;
import com.fairhire.repositories.ResumeRepository;
import com.fairhire.services.ResumeService;
import com.fairhire.services.parser.DuplicateResumeException;
import com.fairhire.util.TestDocumentGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = FairHireApplication.class)
@Transactional
class ResumeIngestionIntegrationTest {

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private ResumeRepository resumeRepository;

    @Test
    @DisplayName("PDF Ingestion: Successfully ingests valid single-page PDF, hashes, and extracts text")
    void testIngestValidPdf() throws IOException {
        byte[] pdfBytes = TestDocumentGenerator.createValidPdf(
                List.of("Alice Smith\nPrincipal Engineer with 8 years in Java, Spring Boot, and Cloud Architecture.")
        );
        MockMultipartFile file = new MockMultipartFile(
                "file", "alice_resume.pdf", "application/pdf", pdfBytes
        );

        ResumeUploadResponse response = resumeService.ingestResume(file, null, "Alice Smith", "alice.smith@example.com");

        assertNotNull(response.resumeId());
        assertEquals(ParsingStatus.READY, response.parsingStatus());
        assertEquals("alice_resume.pdf", response.fileName());
        assertEquals(ResumeFileType.PDF, response.fileType());
        assertNotNull(response.fileHashSha256());
        assertEquals(64, response.fileHashSha256().length());
        assertTrue(response.rawText().contains("Alice Smith"));
        assertTrue(response.rawText().contains("Principal Engineer"));
        assertEquals("v1.0-deterministic", response.parserVersion());
        assertNull(response.parsingError());

        // Verify database persistence
        Resume persisted = resumeRepository.findById(response.resumeId()).orElseThrow();
        assertEquals(ParsingStatus.READY, persisted.getParsingStatus());
        assertEquals(response.fileHashSha256(), persisted.getFileHashSha256());
        assertEquals(response.rawText(), persisted.getRawText());
        assertNotNull(persisted.getFilePath()); // Storage key exists internally
        assertFalse(persisted.getFilePath().contains("..")); // Safe storage key
    }

    @Test
    @DisplayName("PDF Ingestion: Extracts multi-page PDF preserving document order")
    void testIngestMultiPagePdf() throws IOException {
        byte[] pdfBytes = TestDocumentGenerator.createValidPdf(List.of(
                "PAGE 1: Bob Taylor - Profile Summary",
                "PAGE 2: Experience: Staff Architect at TechGlobal",
                "PAGE 3: Education: M.Sc. Computer Science"
        ));
        MockMultipartFile file = new MockMultipartFile(
                "file", "bob_resume.pdf", "application/pdf", pdfBytes
        );

        ResumeUploadResponse response = resumeService.ingestResume(file, null, "Bob Taylor", "bob.taylor@example.com");

        assertEquals(ParsingStatus.READY, response.parsingStatus());
        assertTrue(response.rawText().contains("PAGE 1"));
        assertTrue(response.rawText().contains("PAGE 2"));
        assertTrue(response.rawText().contains("PAGE 3"));

        int idx1 = response.rawText().indexOf("PAGE 1");
        int idx2 = response.rawText().indexOf("PAGE 2");
        int idx3 = response.rawText().indexOf("PAGE 3");
        assertTrue(idx1 < idx2 && idx2 < idx3, "Multi-page content must preserve strict document order");
    }

    @Test
    @DisplayName("PDF Ingestion: Empty/Image-only PDF transitions to FAILED status noting OCR required")
    void testIngestImageOnlyPdfRequiresOcr() throws IOException {
        byte[] emptyPdf = TestDocumentGenerator.createEmptyPdf();
        MockMultipartFile file = new MockMultipartFile(
                "file", "scanned_resume.pdf", "application/pdf", emptyPdf
        );

        ResumeUploadResponse response = resumeService.ingestResume(file, null, "Scanned Candidate", "scanned@example.com");

        assertEquals(ParsingStatus.FAILED, response.parsingStatus());
        assertNotNull(response.parsingError());
        assertTrue(response.parsingError().contains("requires OCR"),
                "Parsing error must explicitly state OCR is required: " + response.parsingError());
        assertEquals("", response.rawText());

        // Verify entity persisted in FAILED state with error diagnostic
        Resume persisted = resumeRepository.findById(response.resumeId()).orElseThrow();
        assertEquals(ParsingStatus.FAILED, persisted.getParsingStatus());
        assertNotNull(persisted.getParsingError());
    }

    @Test
    @DisplayName("DOCX Ingestion: Extracts paragraphs and tables in document order")
    void testIngestDocxWithTable() throws IOException {
        byte[] docxBytes = TestDocumentGenerator.createTableDocx(
                List.of("Carol White", "Senior Developer"),
                List.of(
                        List.of("Technology", "Years"),
                        List.of("Java", "6"),
                        List.of("PostgreSQL", "5")
                )
        );
        MockMultipartFile file = new MockMultipartFile(
                "file", "carol_resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxBytes
        );

        ResumeUploadResponse response = resumeService.ingestResume(file, null, "Carol White", "carol.white@example.com");

        assertEquals(ParsingStatus.READY, response.parsingStatus());
        assertEquals(ResumeFileType.DOCX, response.fileType());
        assertTrue(response.rawText().contains("Carol White"));
        assertTrue(response.rawText().contains("Technology | Years"));
        assertTrue(response.rawText().contains("Java | 6"));
    }

    @Test
    @DisplayName("Duplicate Detection: Rejects second upload of identical document with DuplicateResumeException")
    void testRejectDuplicateHashUpload() throws IOException {
        byte[] pdfBytes = TestDocumentGenerator.createValidPdf(List.of("Unique Resume Content For Duplicate Test"));
        MockMultipartFile file1 = new MockMultipartFile("file", "resume_v1.pdf", "application/pdf", pdfBytes);
        MockMultipartFile file2 = new MockMultipartFile("file", "resume_v2.pdf", "application/pdf", pdfBytes);

        // First upload succeeds
        ResumeUploadResponse r1 = resumeService.ingestResume(file1, null, "Candidate One", "cand1@example.com");
        assertNotNull(r1.resumeId());

        // Second upload with identical bytes/hash must be rejected
        DuplicateResumeException ex = assertThrows(DuplicateResumeException.class, () ->
                resumeService.ingestResume(file2, null, "Candidate Two", "cand2@example.com")
        );
        assertEquals(r1.fileHashSha256(), ex.getSha256());
        assertEquals(r1.resumeId(), ex.getExistingResumeId());
    }

    @Test
    @DisplayName("Persistence: Preserves original SHA-256 hash across updates")
    void testSha256Immutability() throws IOException {
        byte[] pdfBytes = TestDocumentGenerator.createValidPdf(List.of("Immutable Hash Resume Test"));
        MockMultipartFile file = new MockMultipartFile("file", "immutable.pdf", "application/pdf", pdfBytes);

        ResumeUploadResponse response = resumeService.ingestResume(file, null, "Immutability Candidate", "imm@example.com");
        String originalHash = response.fileHashSha256();

        Resume entity = resumeRepository.findById(response.resumeId()).orElseThrow();
        assertEquals(originalHash, entity.getFileHashSha256());

        // Update other metadata
        entity.setFileName("renamed_resume.pdf");
        resumeRepository.save(entity);

        Resume reloaded = resumeRepository.findById(response.resumeId()).orElseThrow();
        assertEquals(originalHash, reloaded.getFileHashSha256(), "Original cryptographic hash must not change");
    }

    @Test
    @DisplayName("B-06: Ingesting whitespace-only text file upload throws InvalidResumeUploadException")
    void testIngestWhitespaceFileThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "whitespace.txt", "text/plain", "   \r\n\t  \n".getBytes()
        );

        assertThrows(com.fairhire.services.parser.InvalidResumeUploadException.class, () ->
                resumeService.ingestResume(file, null, "Blank User", "blank@example.com")
        );
    }

    @Test
    @DisplayName("B-06: Ingesting raw text containing only whitespace or BOM throws InvalidResumeUploadException")
    void testIngestBlankRawTextThrowsException() {
        assertThrows(com.fairhire.services.parser.InvalidResumeUploadException.class, () ->
                resumeService.ingestRawText("   \n\t   ", "Blank User", "blank2@example.com")
        );

        assertThrows(com.fairhire.services.parser.InvalidResumeUploadException.class, () ->
                resumeService.ingestRawText("\uFEFF  \r\n  ", "BOM User", "bom@example.com")
        );
    }
}
