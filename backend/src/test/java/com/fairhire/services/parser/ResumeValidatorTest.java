package com.fairhire.services.parser;

import com.fairhire.models.enums.ResumeFileType;
import com.fairhire.util.TestDocumentGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResumeValidatorTest {

    private ResumeValidator validator;

    @BeforeEach
    void setUp() {
        // 10 MB limit = 10,485,760 bytes
        validator = new ResumeValidator(10485760L);
    }

    @Test
    @DisplayName("Accepts valid PDF upload with valid %PDF- magic bytes and MIME")
    void testValidPdf() throws IOException {
        byte[] pdfBytes = TestDocumentGenerator.createValidPdf(List.of("Resume content"));
        MockMultipartFile file = new MockMultipartFile(
                "file", "candidate_resume.pdf", "application/pdf", pdfBytes
        );

        ResumeFileType type = validator.validate(file);
        assertEquals(ResumeFileType.PDF, type);
    }

    @Test
    @DisplayName("Accepts valid DOCX upload with valid PK magic bytes and MIME")
    void testValidDocx() throws IOException {
        byte[] docxBytes = TestDocumentGenerator.createValidDocx(List.of("Resume content"));
        MockMultipartFile file = new MockMultipartFile(
                "file", "candidate_resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxBytes
        );

        ResumeFileType type = validator.validate(file);
        assertEquals(ResumeFileType.DOCX, type);
    }

    @Test
    @DisplayName("Rejects empty file upload")
    void testEmptyFileUpload() {
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[0]);
        InvalidResumeUploadException ex = assertThrows(InvalidResumeUploadException.class, () ->
                validator.validate(file)
        );
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    @DisplayName("Rejects oversized file upload exceeding 10MB")
    void testOversizedFileUpload() {
        // Small validator with 100 byte limit for test
        ResumeValidator smallValidator = new ResumeValidator(100L);
        byte[] largeBytes = new byte[150];

        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", largeBytes);
        InvalidResumeUploadException ex = assertThrows(InvalidResumeUploadException.class, () ->
                smallValidator.validate(file)
        );
        assertTrue(ex.getMessage().contains("exceeds maximum permitted limit"));
    }

    @Test
    @DisplayName("Rejects unsupported file extension (.exe, .sh, .jpg)")
    void testUnsupportedExtension() {
        MockMultipartFile exeFile = new MockMultipartFile("file", "resume.exe", "application/octet-stream", "not real".getBytes());
        InvalidResumeUploadException ex1 = assertThrows(InvalidResumeUploadException.class, () ->
                validator.validate(exeFile)
        );
        assertTrue(ex1.getMessage().contains("Unsupported file extension"));

        MockMultipartFile shFile = new MockMultipartFile("file", "script.sh", "text/x-shellscript", "#!/bin/bash".getBytes());
        assertThrows(InvalidResumeUploadException.class, () -> validator.validate(shFile));

        MockMultipartFile imgFile = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "image bytes".getBytes());
        assertThrows(InvalidResumeUploadException.class, () -> validator.validate(imgFile));
    }

    @Test
    @DisplayName("Rejects spoofed PDF with missing %PDF- magic bytes")
    void testInvalidMagicBytesPdf() {
        // File named .pdf but containing plain text or random binary bytes
        MockMultipartFile spoofedFile = new MockMultipartFile(
                "file", "fake.pdf", "application/pdf", "This is plain text not a PDF file.".getBytes()
        );
        InvalidResumeUploadException ex = assertThrows(InvalidResumeUploadException.class, () ->
                validator.validate(spoofedFile)
        );
        assertTrue(ex.getMessage().contains("lacks standard PDF header signature"));
    }

    @Test
    @DisplayName("Rejects spoofed DOCX with missing PK magic bytes")
    void testInvalidMagicBytesDocx() {
        MockMultipartFile spoofedFile = new MockMultipartFile(
                "file", "fake.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "Not a valid zip container".getBytes()
        );
        InvalidResumeUploadException ex = assertThrows(InvalidResumeUploadException.class, () ->
                validator.validate(spoofedFile)
        );
        assertTrue(ex.getMessage().contains("lacks OpenXML/ZIP header signature"));
    }

    @Test
    @DisplayName("Security: Rejects path traversal filename attempts")
    void testSecurityPathTraversalFilename() {
        byte[] payload = "%PDF-1.4\nvalid mock".getBytes();
        MockMultipartFile file1 = new MockMultipartFile("file", "../../etc/passwd.pdf", "application/pdf", payload);
        InvalidResumeUploadException ex1 = assertThrows(InvalidResumeUploadException.class, () ->
                validator.validate(file1)
        );
        assertTrue(ex1.getMessage().contains("path traversal"));

        MockMultipartFile file2 = new MockMultipartFile("file", "..\\..\\windows\\system32\\cmd.pdf", "application/pdf", payload);
        assertThrows(InvalidResumeUploadException.class, () -> validator.validate(file2));
    }

    @Test
    @DisplayName("Security: Rejects command injection characters in filename")
    void testSecurityCommandInjectionFilename() {
        byte[] payload = "%PDF-1.4\nvalid mock".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "resume;rm -rf;.pdf", "application/pdf", payload);
        InvalidResumeUploadException ex = assertThrows(InvalidResumeUploadException.class, () ->
                validator.validate(file)
        );
        assertTrue(ex.getMessage().contains("suspicious command characters"));
    }

    @Test
    @DisplayName("Security: Rejects executable binaries (MZ header)")
    void testSecurityExecutableBinaryPayload() {
        byte[] mzPayload = TestDocumentGenerator.createExecutablePayload();
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", mzPayload);
        InvalidResumeUploadException ex = assertThrows(InvalidResumeUploadException.class, () ->
                validator.validate(file)
        );
        assertTrue(ex.getMessage().contains("Windows executable (MZ) binary detected"));
    }

    @Test
    @DisplayName("B-06: Rejects text file containing only whitespace")
    void testWhitespaceOnlyTextFileRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "whitespace.txt", "text/plain", "   \n\n\t  ".getBytes()
        );
        InvalidResumeUploadException ex = assertThrows(InvalidResumeUploadException.class, () ->
                validator.validate(file)
        );
        assertTrue(ex.getMessage().contains("only whitespace or is empty"));
    }

    @Test
    @DisplayName("B-06: Rejects text file containing BOM with whitespace only")
    void testBomWhitespaceTextFileRejected() {
        byte[] bomWhitespace = "\uFEFF   \r\n\t ".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "bom_whitespace.txt", "text/plain", bomWhitespace
        );
        InvalidResumeUploadException ex = assertThrows(InvalidResumeUploadException.class, () ->
                validator.validate(file)
        );
        assertTrue(ex.getMessage().contains("only whitespace or is empty"));
    }

    @Test
    @DisplayName("B-06: Accepts valid text file with actual non-whitespace content")
    void testValidTextFileAccepted() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "valid_resume.txt", "text/plain", "Experienced Java Developer".getBytes()
        );
        ResumeFileType type = validator.validate(file);
        assertEquals(ResumeFileType.TXT, type);
    }
}
