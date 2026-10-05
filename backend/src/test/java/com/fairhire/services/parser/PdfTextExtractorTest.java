package com.fairhire.services.parser;

import com.fairhire.models.enums.ResumeFileType;
import com.fairhire.util.TestDocumentGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfTextExtractorTest {

    private PdfTextExtractor pdfTextExtractor;

    @BeforeEach
    void setUp() {
        pdfTextExtractor = new PdfTextExtractor();
    }

    @Test
    @DisplayName("Supports PDF file type only")
    void testSupports() {
        assertTrue(pdfTextExtractor.supports(ResumeFileType.PDF));
        assertFalse(pdfTextExtractor.supports(ResumeFileType.DOCX));
        assertFalse(pdfTextExtractor.supports(ResumeFileType.TXT));
    }

    @Test
    @DisplayName("Extracts text from normal single-page PDF")
    void testExtractNormalSinglePagePdf() throws IOException {
        byte[] pdfBytes = TestDocumentGenerator.createValidPdf(
                List.of("Jane Doe\nSenior Software Engineer with 5 years experience in Java and Spring Boot.")
        );

        ExtractionResult result = pdfTextExtractor.extract(pdfBytes);
        assertNotNull(result);
        assertEquals(1, result.pageOrElementCount());
        assertTrue(result.rawText().contains("Jane Doe"));
        assertTrue(result.rawText().contains("Senior Software Engineer"));
        assertEquals("Apache PDFBox 3.0.3", result.parserEngine());
        assertFalse(result.isScannedOrEmpty());
    }

    @Test
    @DisplayName("Extracts multi-page PDF preserving page document order")
    void testExtractMultiPagePdfPreservesOrder() throws IOException {
        byte[] pdfBytes = TestDocumentGenerator.createValidPdf(List.of(
                "PAGE ONE: Jane Doe - Education: B.S. Computer Science",
                "PAGE TWO: Experience: Tech Lead at Acme Corp (2020-2024)",
                "PAGE THREE: Skills: Java, Spring Boot, Docker, PostgreSQL"
        ));

        ExtractionResult result = pdfTextExtractor.extract(pdfBytes);
        assertNotNull(result);
        assertEquals(3, result.pageOrElementCount());

        String text = result.rawText();
        assertTrue(text.contains("PAGE ONE"));
        assertTrue(text.contains("PAGE TWO"));
        assertTrue(text.contains("PAGE THREE"));

        // Verify strict reading order: Page 1 before Page 2, Page 2 before Page 3
        int idx1 = text.indexOf("PAGE ONE");
        int idx2 = text.indexOf("PAGE TWO");
        int idx3 = text.indexOf("PAGE THREE");
        assertTrue(idx1 < idx2, "Page 1 must appear before Page 2");
        assertTrue(idx2 < idx3, "Page 2 must appear before Page 3");
    }

    @Test
    @DisplayName("Detects empty or image-only PDF and throws OcrRequiredException")
    void testExtractEmptyOrImageOnlyPdf() throws IOException {
        byte[] emptyPdf = TestDocumentGenerator.createEmptyPdf();

        OcrRequiredException ex = assertThrows(OcrRequiredException.class, () ->
                pdfTextExtractor.extract(emptyPdf)
        );
        assertTrue(ex.getMessage().contains("requires OCR"),
                "Exception message must clearly state OCR is required: " + ex.getMessage());
    }

    @Test
    @DisplayName("Handles malformed PDF safely and throws TextExtractionException")
    void testExtractMalformedPdf() {
        byte[] malformedPdf = TestDocumentGenerator.createMalformedPdf();

        TextExtractionException ex = assertThrows(TextExtractionException.class, () ->
                pdfTextExtractor.extract(malformedPdf)
        );
        assertNotNull(ex.getMessage());
    }
}
