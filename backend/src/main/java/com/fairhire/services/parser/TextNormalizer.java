package com.fairhire.services.parser;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Deterministic text normalizer for extracted resume content.
 * Standardizes line endings, strips extraction artifacts, collapses redundant whitespace,
 * while strictly preserving original textual structure, casing, and section boundaries.
 */
@Component
public class TextNormalizer {

    private static final Pattern CARRIAGE_RETURNS = Pattern.compile("\r\n|\r");
    private static final Pattern NON_BREAKING_SPACES = Pattern.compile("[\u00A0\u2007\u202F]");
    private static final Pattern ZERO_WIDTH_CHARS = Pattern.compile("[\u200B\u200C\u200D\uFEFF\u00AD\uFFFC\uFFFD]");
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F]");
    private static final Pattern HORIZONTAL_WHITESPACE = Pattern.compile("[ \t]+");
    private static final Pattern TRAILING_LINE_SPACES = Pattern.compile("[ \t]+$", Pattern.MULTILINE);
    private static final Pattern EXCESSIVE_NEWLINES = Pattern.compile("\n{3,}");

    /**
     * Normalizes raw extracted text deterministically.
     *
     * @param rawText Extracted text from PDF, DOCX, or TXT
     * @return Deterministically normalized text string
     */
    public String normalize(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        // 1. Standardize line endings to \n
        String text = CARRIAGE_RETURNS.matcher(rawText).replaceAll("\n");

        // 2. Remove non-printable control characters and zero-width artifacts
        text = ZERO_WIDTH_CHARS.matcher(text).replaceAll("");
        text = CONTROL_CHARS.matcher(text).replaceAll("");

        // 3. Replace non-breaking spaces with standard spaces
        text = NON_BREAKING_SPACES.matcher(text).replaceAll(" ");

        // 4. Normalize multiple horizontal spaces/tabs on lines while preserving newlines
        String[] lines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            // Collapse multiple spaces within the line
            String trimmedLine = HORIZONTAL_WHITESPACE.matcher(line).replaceAll(" ").trim();
            sb.append(trimmedLine);
            if (i < lines.length - 1) {
                sb.append("\n");
            }
        }
        text = sb.toString();

        // 5. Strip trailing line spaces
        text = TRAILING_LINE_SPACES.matcher(text).replaceAll("");

        // 6. Collapse excessive blank lines (more than 2 consecutive newlines -> 2 newlines)
        text = EXCESSIVE_NEWLINES.matcher(text).replaceAll("\n\n");

        return text.trim();
    }
}
