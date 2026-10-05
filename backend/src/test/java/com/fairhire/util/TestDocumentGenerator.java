package com.fairhire.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Test utility generating valid and invalid in-memory PDF and DOCX binaries.
 */
public class TestDocumentGenerator {

    public static byte[] createValidPdf(List<String> pagesContent) throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            for (String text : pagesContent) {
                PDPage page = new PDPage();
                doc.addPage(page);

                try (PDPageContentStream cos = new PDPageContentStream(doc, page)) {
                    cos.beginText();
                    cos.setFont(font, 12);
                    cos.newLineAtOffset(100, 700);
                    String[] lines = text.split("\r?\n");
                    for (int l = 0; l < lines.length; l++) {
                        if (l > 0) {
                            cos.newLineAtOffset(0, -15);
                        }
                        cos.showText(lines[l]);
                    }
                    cos.endText();
                }
            }

            doc.save(baos);
            return baos.toByteArray();
        }
    }

    public static byte[] createEmptyPdf() throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page); // 1 page with no text content stream
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    public static byte[] createMalformedPdf() {
        return "%PDF-1.4\n%corrupt bytes without valid objects or xref tables\n%%EOF".getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] createValidDocx(List<String> paragraphs) throws IOException {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            for (String text : paragraphs) {
                XWPFParagraph p = doc.createParagraph();
                p.createRun().setText(text);
            }

            doc.write(baos);
            return baos.toByteArray();
        }
    }

    public static byte[] createTableDocx(List<String> introParagraphs, List<List<String>> tableData) throws IOException {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            for (String text : introParagraphs) {
                doc.createParagraph().createRun().setText(text);
            }

            if (tableData != null && !tableData.isEmpty()) {
                XWPFTable table = doc.createTable();
                for (int r = 0; r < tableData.size(); r++) {
                    List<String> rowCells = tableData.get(r);
                    XWPFTableRow row = (r == 0) ? table.getRow(0) : table.createRow();
                    for (int c = 0; c < rowCells.size(); c++) {
                        XWPFTableCell cell = (c < row.getTableCells().size()) ? row.getCell(c) : row.addNewTableCell();
                        // Clear existing empty text if any and set content
                        cell.setText(rowCells.get(c));
                    }
                }
            }

            doc.write(baos);
            return baos.toByteArray();
        }
    }

    public static byte[] createMalformedDocx() {
        // Starts with PK zip magic bytes but truncated/invalid zip structure
        return new byte[]{0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    }

    public static byte[] createExecutablePayload() {
        // Starts with DOS PE 'MZ' magic bytes
        return new byte[]{0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00};
    }
}
