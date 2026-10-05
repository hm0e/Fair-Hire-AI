package com.fairhire.services.parser;

import com.fairhire.models.enums.ResumeFileType;

/**
 * Strategy interface for deterministic text extraction from various resume document formats.
 */
public interface ResumeTextExtractor {

    /**
     * Check if this extractor supports the given file format.
     */
    boolean supports(ResumeFileType fileType);

    /**
     * Deterministically extract text from raw file bytes.
     *
     * @param fileBytes Binary content of the document
     * @return {@link ExtractionResult} containing extracted text and metadata
     * @throws TextExtractionException If extraction fails or document is corrupt
     * @throws OcrRequiredException    If document is scanned / image-only with no extractable text
     */
    ExtractionResult extract(byte[] fileBytes) throws TextExtractionException;
}
