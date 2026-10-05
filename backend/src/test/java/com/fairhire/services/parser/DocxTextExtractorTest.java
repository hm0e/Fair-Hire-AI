package com.fairhire.services.parser;

import com.fairhire.models.enums.ResumeFileType;
import com.fairhire.util.TestDocumentGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocxTextExtractorTest {

    private DocxTextExtractor docxTextExtractor;

    @BeforeEach
    void setUp() {
        docxTextExtractor = new DocxTextExtractor();
    }

    @Test
    @DisplayName("Supports DOCX file type only")
    void testSupports() {
        assertTrue(docxTextExtractor.supports(ResumeFileType.DOCX));
        assertFalse(docxTextExtractor.supports(ResumeFileType.PDF));
        assertFalse(docxTextExtractor.supports(ResumeFileType.TXT));
    }

    @Test
    @DisplayName("Extracts paragraphs and headings from normal DOCX")
    void testExtractNormalDocx() throws IOException {
        byte[] docxBytes = TestDocumentGenerator.createValidDocx(List.of(
                "John Smith",
                "Senior Backend Engineer",
                "Summary: 6 years building high-throughput distributed systems in Java."
        ));

        ExtractionResult result = docxTextExtractor.extract(docxBytes);
        assertNotNull(result);
        assertEquals("Apache POI 5.3.0", result.parserEngine());
        assertTrue(result.rawText().contains("John Smith"));
        assertTrue(result.rawText().contains("Senior Backend Engineer"));
        assertTrue(result.rawText().contains("Summary: 6 years"));
    }

    @Test
    @DisplayName("Extracts table data in document order from table-containing DOCX")
    void testExtractTableContainingDocx() throws IOException {
        List<String> paragraphs = List.of(
                "Candidate: Alice Johnson",
                "Skill Proficiency Summary:"
        );
        List<List<String>> table = List.of(
                List.of("Skill", "Proficiency Level", "Years Experience"),
                List.of("Java", "Expert", "5"),
                List.of("PostgreSQL", "Advanced", "4"),
                List.of("Docker", "Intermediate", "3")
        );

        byte[] docxBytes = TestDocumentGenerator.createTableDocx(paragraphs, table);
        ExtractionResult result = docxTextExtractor.extract(docxBytes);
        assertNotNull(result);

        String text = result.rawText();
        assertTrue(text.contains("Candidate: Alice Johnson"));
        assertTrue(text.contains("Skill | Proficiency Level | Years Experience"));
        assertTrue(text.contains("Java | Expert | 5"));
        assertTrue(text.contains("PostgreSQL | Advanced | 4"));
        assertTrue(text.contains("Docker | Intermediate | 3"));

        // Document order check: Heading appears before Table content
        int headingIdx = text.indexOf("Candidate: Alice Johnson");
        int tableIdx = text.indexOf("Java | Expert | 5");
        assertTrue(headingIdx < tableIdx, "Paragraphs must appear before subsequent table rows");
    }

    @Test
    @DisplayName("Handles malformed DOCX safely and throws TextExtractionException")
    void testExtractMalformedDocx() {
        byte[] malformedDocx = TestDocumentGenerator.createMalformedDocx();

        TextExtractionException ex = assertThrows(TextExtractionException.class, () ->
                docxTextExtractor.extract(malformedDocx)
        );
        assertNotNull(ex.getMessage());
    }
}
