package com.fairhire.services.parser;

/**
 * Encapsulates the output of a deterministic document parser.
 */
public record ExtractionResult(
        String rawText,
        int pageOrElementCount,
        String parserEngine,
        boolean isScannedOrEmpty
) {}
