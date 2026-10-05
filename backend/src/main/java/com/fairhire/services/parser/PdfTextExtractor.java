package com.fairhire.services.parser;

import com.fairhire.models.enums.ResumeFileType;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Deterministic PDF text extractor using Apache PDFBox 3.0.3.
 * Preserves reading order by sorting characters by coordinate position.
 * Detects image-only/scanned documents lacking extractable textual content.
 */
@Component
public class PdfTextExtractor implements ResumeTextExtractor {

    @Override
    public boolean supports(ResumeFileType fileType) {
        return fileType == ResumeFileType.PDF;
    }

    @Override
    public ExtractionResult extract(byte[] fileBytes) throws TextExtractionException {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new TextExtractionException("Cannot extract text from empty PDF payload.");
        }

        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            if (document.isEncrypted()) {
                throw new TextExtractionException("Upload rejected: PDF is encrypted or password-protected.");
            }

            int pageCount = document.getNumberOfPages();
            if (pageCount == 0) {
                throw new TextExtractionException("Upload rejected: PDF document contains 0 pages.");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            // Preserve visual document and multi-column reading order
            stripper.setSortByPosition(true);

            String text = stripper.getText(document);

            if (text == null || text.trim().isBlank()) {
                throw new OcrRequiredException(
                        "PDF contains no extractable text; document appears to be scanned or image-only and requires OCR."
                );
            }

            return new ExtractionResult(text, pageCount, "Apache PDFBox 3.0.3", false);

        } catch (OcrRequiredException e) {
            throw e;
        } catch (IOException e) {
            throw new TextExtractionException("Malformed or unreadable PDF document: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new TextExtractionException("Unexpected error during PDF text extraction: " + e.getMessage(), e);
        }
    }
}
