package com.fairhire.services.matching;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Deterministic PII Scrubber for evidence snippets.
 * Ensures candidate context snippets and matched text stored in match_skill_details do not leak PII.
 * Adheres strictly to Phase 4 Architecture B (Sanitized Before Persistence).
 * Context snippets bounded to VARCHAR(300), matched text bounded to VARCHAR(100).
 */
@Component
public class DeterministicPiiScrubber {

    public static final int MAX_CONTEXT_SNIPPET_LENGTH = 300;
    public static final int MAX_MATCHED_TEXT_LENGTH = 100;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "(?i)\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}|\\b\\d{3}[-.\\s]\\d{4}\\b"
    );

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)\\bhttps?://[^\\s,]+|\\bwww\\.[^\\s,]+|\\b(?:linkedin\\.com|github\\.com|twitter\\.com)/[^\\s,]+"
    );

    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
            "(?i)\\b\\d{1,5}\\s+[A-Za-z0-9\\s,.]+(?:Street|St\\b|Avenue|Ave\\b|Road|Rd\\b|Boulevard|Blvd\\b|Lane|Ln\\b|Drive|Dr\\b|Court|Ct\\b|Way\\b|Terrace|Ter\\b)"
    );

    private static final Pattern ZIP_PATTERN = Pattern.compile(
            "\\b\\d{5}(?:-\\d{4})?\\b"
    );

    /**
     * Sanitizes raw evidence context snippet by redacting personal identifiers and bounding length to 300 chars.
     *
     * @param snippet Raw text snippet from resume extraction
     * @return Sanitized snippet free of email, phone, url, address, truncated to max 300 chars
     */
    public static String scrubContextSnippet(String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return null;
        }

        String result = redactPii(snippet);

        if (result.length() > MAX_CONTEXT_SNIPPET_LENGTH) {
            result = result.substring(0, MAX_CONTEXT_SNIPPET_LENGTH).trim();
        }

        return result;
    }

    /**
     * Sanitizes matched text snippet by redacting personal identifiers and bounding length to 100 chars.
     *
     * @param matchedText Raw matched text from resume extraction
     * @return Sanitized matched text free of email, phone, url, address, truncated to max 100 chars
     */
    public static String scrubMatchedText(String matchedText) {
        if (matchedText == null || matchedText.isBlank()) {
            return null;
        }

        String result = redactPii(matchedText);

        if (result.length() > MAX_MATCHED_TEXT_LENGTH) {
            result = result.substring(0, MAX_MATCHED_TEXT_LENGTH).trim();
        }

        return result;
    }

    private static String redactPii(String input) {
        String result = EMAIL_PATTERN.matcher(input).replaceAll("[REDACTED_EMAIL]");
        result = PHONE_PATTERN.matcher(result).replaceAll("[REDACTED_PHONE]");
        result = URL_PATTERN.matcher(result).replaceAll("[REDACTED_URL]");
        result = ADDRESS_PATTERN.matcher(result).replaceAll("[REDACTED_ADDRESS]");
        result = ZIP_PATTERN.matcher(result).replaceAll("[REDACTED_ADDRESS]");
        return result;
    }

    /**
     * Backwards-compatible scrub method defaulting to context snippet scrubbing (max 300 chars).
     *
     * @param snippet Raw text snippet from resume extraction
     * @return Sanitized snippet truncated to max 300 chars
     */
    public static String scrub(String snippet) {
        return scrubContextSnippet(snippet);
    }
}
