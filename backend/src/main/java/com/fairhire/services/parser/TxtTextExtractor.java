package com.fairhire.services.parser;

import com.fairhire.models.enums.ResumeFileType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Text extractor for plain text (.txt) resumes with standard UTF-8 decoding.
 */
@Component
public class TxtTextExtractor implements ResumeTextExtractor {

    @Override
    public boolean supports(ResumeFileType fileType) {
        return fileType == ResumeFileType.TXT;
    }

    @Override
    public ExtractionResult extract(byte[] fileBytes) throws TextExtractionException {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new TextExtractionException("Cannot extract text from empty text file payload.");
        }

        String content = new String(fileBytes, StandardCharsets.UTF_8).trim();
        if (content.isBlank()) {
            throw new TextExtractionException("Text document contains no extractable characters.");
        }

        return new ExtractionResult(content, 1, "UTF-8 Plain Text Parser", false);
    }
}
