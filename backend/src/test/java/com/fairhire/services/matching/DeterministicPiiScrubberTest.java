package com.fairhire.services.matching;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 4B: Deterministic PII Scrubber Unit Tests")
class DeterministicPiiScrubberTest {

    @Test
    @DisplayName("Redacts Email Addresses with [REDACTED_EMAIL]")
    void testEmailRedaction() {
        String input = "Contact John at john.doe@example.com or support@fairhire.ai for info.";
        String scrubbed = DeterministicPiiScrubber.scrub(input);

        assertFalse(scrubbed.contains("john.doe@example.com"));
        assertFalse(scrubbed.contains("support@fairhire.ai"));
        assertTrue(scrubbed.contains("[REDACTED_EMAIL]"));
    }

    @Test
    @DisplayName("Redacts Phone Numbers with [REDACTED_PHONE]")
    void testPhoneRedaction() {
        String input = "Call 555-1234 or +1 (555) 019-2834 or 555-0199 for verification.";
        String scrubbed = DeterministicPiiScrubber.scrub(input);

        assertFalse(scrubbed.contains("555-1234"));
        assertFalse(scrubbed.contains("555-0199"));
        assertTrue(scrubbed.contains("[REDACTED_PHONE]"));
    }

    @Test
    @DisplayName("Redacts URLs with [REDACTED_URL]")
    void testUrlRedaction() {
        String input = "Portfolio at https://github.com/johndoe and www.johndoe.dev/resume";
        String scrubbed = DeterministicPiiScrubber.scrub(input);

        assertFalse(scrubbed.contains("https://github.com/johndoe"));
        assertFalse(scrubbed.contains("www.johndoe.dev/resume"));
        assertTrue(scrubbed.contains("[REDACTED_URL]"));
    }

    @Test
    @DisplayName("Redacts Physical Addresses and Postal Codes with [REDACTED_ADDRESS]")
    void testAddressRedaction() {
        String input = "Located at 123 Main Street, Suite 400, 94105 San Francisco";
        String scrubbed = DeterministicPiiScrubber.scrub(input);

        assertFalse(scrubbed.contains("123 Main Street"));
        assertFalse(scrubbed.contains("94105"));
        assertTrue(scrubbed.contains("[REDACTED_ADDRESS]"));
    }

    @Test
    @DisplayName("Oracle 1: Context Snippet containing Email and Phone is scrubbed")
    void testOracle1Snippet() {
        String input = "John Doe, john@example.com, 555-1234, Senior Java Dev";
        String scrubbed = DeterministicPiiScrubber.scrub(input);

        assertTrue(scrubbed.contains("[REDACTED_EMAIL]"));
        assertTrue(scrubbed.contains("[REDACTED_PHONE]"));
        assertFalse(scrubbed.contains("john@example.com"));
        assertFalse(scrubbed.contains("555-1234"));
    }

    @Test
    @DisplayName("Oracle L: Truncates context snippets to max 300 characters")
    void testSnippetLengthTruncation() {
        String veryLong = "A".repeat(350);
        String scrubbed = DeterministicPiiScrubber.scrubContextSnippet(veryLong);

        assertEquals(300, scrubbed.length());
    }

    @Test
    @DisplayName("Oracle M: Truncates matched text to max 100 characters")
    void testMatchedTextLengthTruncation() {
        String veryLong = "A".repeat(150);
        String scrubbed = DeterministicPiiScrubber.scrubMatchedText(veryLong);

        assertEquals(100, scrubbed.length());
    }

    @Test
    @DisplayName("Handles null and blank snippets gracefully")
    void testNullAndBlank() {
        assertNull(DeterministicPiiScrubber.scrub(null));
        assertNull(DeterministicPiiScrubber.scrub(""));
        assertNull(DeterministicPiiScrubber.scrub("   "));
    }
}
