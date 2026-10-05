package com.fairhire.services.parser;

import com.fairhire.models.enums.ResumeFileType;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic DOCX text extractor using Apache POI 5.3.0.
 * Traverses body elements sequentially to preserve document layout order across
 * paragraphs, headings, and table cells.
 */
@Component
public class DocxTextExtractor implements ResumeTextExtractor {

    @Override
    public boolean supports(ResumeFileType fileType) {
        return fileType == ResumeFileType.DOCX;
    }

    @Override
    public ExtractionResult extract(byte[] fileBytes) throws TextExtractionException {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new TextExtractionException("Cannot extract text from empty DOCX payload.");
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes);
             XWPFDocument document = new XWPFDocument(bais)) {

            StringBuilder sb = new StringBuilder();
            int elementCount = 0;

            for (IBodyElement element : document.getBodyElements()) {
                elementCount++;
                if (element.getElementType() == BodyElementType.PARAGRAPH) {
                    XWPFParagraph paragraph = (XWPFParagraph) element;
                    String pText = paragraph.getText();
                    if (pText != null && !pText.isBlank()) {
                        sb.append(pText.trim()).append("\n");
                    }
                } else if (element.getElementType() == BodyElementType.TABLE) {
                    XWPFTable table = (XWPFTable) element;
                    for (XWPFTableRow row : table.getRows()) {
                        List<String> cellTexts = new ArrayList<>();
                        for (XWPFTableCell cell : row.getTableCells()) {
                            String cText = cell.getText();
                            if (cText != null) {
                                cellTexts.add(cText.trim());
                            }
                        }
                        if (!cellTexts.isEmpty()) {
                            sb.append(String.join(" | ", cellTexts)).append("\n");
                        }
                    }
                    sb.append("\n");
                }
            }

            String fullText = sb.toString().trim();
            if (fullText.isBlank()) {
                throw new TextExtractionException("DOCX document contains no extractable text.");
            }

            return new ExtractionResult(fullText, elementCount, "Apache POI 5.3.0", false);

        } catch (TextExtractionException e) {
            throw e;
        } catch (IOException e) {
            throw new TextExtractionException("I/O error while reading DOCX stream: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new TextExtractionException("Malformed or corrupt DOCX document: " + e.getMessage(), e);
        }
    }
}
