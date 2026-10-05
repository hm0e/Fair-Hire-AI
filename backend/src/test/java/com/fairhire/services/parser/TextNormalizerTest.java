package com.fairhire.services.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextNormalizerTest {

    private TextNormalizer textNormalizer;

    @BeforeEach
    void setUp() {
        textNormalizer = new TextNormalizer();
    }

    @Test
    @DisplayName("Normalizes Windows CRLF and classic Mac CR line endings to LF")
    void testNormalizeLineEndings() {
        String input = "Line 1\r\nLine 2\rLine 3\nLine 4";
        String normalized = textNormalizer.normalize(input);
        assertEquals("Line 1\nLine 2\nLine 3\nLine 4", normalized);
    }

    @Test
    @DisplayName("Removes zero-width characters, control characters, and normalizes non-breaking spaces")
    void testRemoveArtifactsAndControlChars() {
        // Contains zero-width space \u200B, non-breaking space \u00A0, and bell character \u0007
        String input = "Skill:\u200B Java\u00A0Developer\u0007 with\uFEFF Spring.";
        String normalized = textNormalizer.normalize(input);
        assertEquals("Skill: Java Developer with Spring.", normalized);
    }

    @Test
    @DisplayName("Collapses excessive blank lines to maximum of two newlines")
    void testCollapseExcessiveNewlines() {
        String input = "Header\n\n\n\n\nParagraph 1\n\n\n\nParagraph 2";
        String normalized = textNormalizer.normalize(input);
        assertEquals("Header\n\nParagraph 1\n\nParagraph 2", normalized);
    }

    @Test
    @DisplayName("Collapses multiple horizontal spaces and trims trailing spaces on lines")
    void testTrimHorizontalWhitespace() {
        String input = "Name:    Sarah Connor   \nTitle:   DevOps   Specialist   ";
        String normalized = textNormalizer.normalize(input);
        assertEquals("Name: Sarah Connor\nTitle: DevOps Specialist", normalized);
    }

    @Test
    @DisplayName("Preserves original character casing and meaningful section layout")
    void testPreservesCaseAndStructure() {
        String input = "SUMMARY\nSoftware Engineer with experience in Python, PyTorch, and SQL.\n\nEXPERIENCE\nCompany XYZ";
        String normalized = textNormalizer.normalize(input);
        assertEquals(input, normalized);
    }

    @Test
    @DisplayName("Handles null and blank inputs safely")
    void testNullAndBlankInput() {
        assertEquals("", textNormalizer.normalize(null));
        assertEquals("", textNormalizer.normalize("   \n\t   "));
    }
}
