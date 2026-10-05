package com.fairhire.services.parser;

import com.fairhire.models.enums.ResumeFileType;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Validates resume file uploads for size, extension, MIME type, magic bytes, and path traversal security.
 */
@Component
public class ResumeValidator {

    private final long maxFileSizeBytes;

    // Magic byte signatures
    private static final byte[] PDF_MAGIC = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}; // %PDF-
    private static final byte[] PK_MAGIC = new byte[]{0x50, 0x4B, 0x03, 0x04}; // PK\x03\x04 (ZIP / DOCX)

    // Forbidden executable signatures
    private static final byte[] DOS_MZ_MAGIC = new byte[]{0x4D, 0x5A}; // MZ
    private static final byte[] ELF_MAGIC = new byte[]{0x7F, 0x45, 0x4C, 0x46}; // \x7fELF
    private static final byte[] SHEBANG_MAGIC = new byte[]{0x23, 0x21}; // #!

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "txt");

    private static final List<String> ALLOWED_PDF_MIMES = List.of(
            "application/pdf", "application/x-pdf", "application/acrobat", "application/octet-stream"
    );

    private static final List<String> ALLOWED_DOCX_MIMES = List.of(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/zip",
            "application/x-zip-compressed",
            "application/octet-stream"
    );

    private static final List<String> ALLOWED_TXT_MIMES = List.of(
            "text/plain", "text/markdown", "application/octet-stream"
    );

    public ResumeValidator(
            @Value("${fairhire.upload.max-file-size-bytes:10485760}") long maxFileSizeBytes
    ) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    /**
     * Complete validation pipeline: size, filename, extension, MIME type, magic byte signatures.
     *
     * @param file Uploaded multipart file
     * @return Validated {@link ResumeFileType}
     * @throws InvalidResumeUploadException If any check fails
     */
    public ResumeFileType validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new InvalidResumeUploadException("Upload rejected: File is empty or not provided.");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new InvalidResumeUploadException(String.format(
                    "Upload rejected: File size (%d bytes) exceeds maximum permitted limit (%d bytes).",
                    file.getSize(), maxFileSizeBytes
            ));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new InvalidResumeUploadException("Upload rejected: Missing filename.");
        }

        // Security check: Path traversal protection
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\") || originalFilename.contains("\0")) {
            throw new InvalidResumeUploadException("Security violation: Filename contains path traversal or illegal characters: " + originalFilename);
        }

        // Security check: Suspicious command injection characters
        if (originalFilename.matches(".*[;&|`$<>].*")) {
            throw new InvalidResumeUploadException("Security violation: Filename contains suspicious command characters: " + originalFilename);
        }

        String extension = FilenameUtils.getExtension(originalFilename);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new InvalidResumeUploadException("Upload rejected: Unsupported file extension '." + extension +
                    "'. Allowed formats are: .pdf, .docx, .txt");
        }

        byte[] headerBytes;
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
            if (fileBytes.length < 4) {
                throw new InvalidResumeUploadException("Upload rejected: Corrupt file (payload smaller than 4 bytes).");
            }
            headerBytes = Arrays.copyOf(fileBytes, Math.min(fileBytes.length, 1024));
        } catch (IOException e) {
            throw new InvalidResumeUploadException("Upload rejected: Failed to read file stream: " + e.getMessage(), e);
        }

        // Security check: Reject executable payloads
        if (startsWithBytes(headerBytes, DOS_MZ_MAGIC)) {
            throw new InvalidResumeUploadException("Security violation: Windows executable (MZ) binary detected.");
        }
        if (startsWithBytes(headerBytes, ELF_MAGIC)) {
            throw new InvalidResumeUploadException("Security violation: Linux executable (ELF) binary detected.");
        }
        if (startsWithBytes(headerBytes, SHEBANG_MAGIC)) {
            throw new InvalidResumeUploadException("Security violation: Executable script (#!) detected.");
        }

        String mimeType = file.getContentType();
        String cleanExt = extension.toLowerCase();

        switch (cleanExt) {
            case "pdf" -> {
                validatePdf(headerBytes, mimeType);
                return ResumeFileType.PDF;
            }
            case "docx" -> {
                validateDocx(headerBytes, mimeType);
                return ResumeFileType.DOCX;
            }
            case "txt" -> {
                validateTxt(headerBytes, fileBytes, mimeType);
                return ResumeFileType.TXT;
            }
            default -> throw new InvalidResumeUploadException("Unsupported file type: " + cleanExt);
        }
    }

    private void validatePdf(byte[] headerBytes, String mimeType) {
        if (!startsWithBytes(headerBytes, PDF_MAGIC)) {
            throw new InvalidResumeUploadException(
                    "Upload rejected: File extension is .pdf but content lacks standard PDF header signature (%PDF-)."
            );
        }
        if (mimeType != null && !mimeType.isBlank()) {
            String lowerMime = mimeType.toLowerCase();
            boolean isAllowed = ALLOWED_PDF_MIMES.stream().anyMatch(lowerMime::startsWith);
            if (!isAllowed) {
                throw new InvalidResumeUploadException(
                        "Upload rejected: Disallowed MIME type '" + mimeType + "' for PDF document."
                );
            }
        }
    }

    private void validateDocx(byte[] headerBytes, String mimeType) {
        if (!startsWithBytes(headerBytes, PK_MAGIC)) {
            throw new InvalidResumeUploadException(
                    "Upload rejected: File extension is .docx but content lacks OpenXML/ZIP header signature (PK)."
            );
        }
        if (mimeType != null && !mimeType.isBlank()) {
            String lowerMime = mimeType.toLowerCase();
            boolean isAllowed = ALLOWED_DOCX_MIMES.stream().anyMatch(lowerMime::startsWith);
            if (!isAllowed) {
                throw new InvalidResumeUploadException(
                        "Upload rejected: Disallowed MIME type '" + mimeType + "' for DOCX document."
                );
            }
        }
    }

    private void validateTxt(byte[] headerBytes, byte[] fileBytes, String mimeType) {
        if (mimeType != null && !mimeType.isBlank()) {
            String lowerMime = mimeType.toLowerCase();
            boolean isAllowed = ALLOWED_TXT_MIMES.stream().anyMatch(lowerMime::startsWith);
            if (!isAllowed) {
                throw new InvalidResumeUploadException(
                        "Upload rejected: Disallowed MIME type '" + mimeType + "' for text document."
                );
            }
        }
        // Verify no binary control bytes (NUL byte) in initial text segment
        for (byte b : headerBytes) {
            if (b == 0x00) {
                throw new InvalidResumeUploadException("Upload rejected: Binary / null bytes detected in plain text file.");
            }
        }
        // Reject blank or whitespace-only text files (including BOM + whitespace)
        String content = new String(fileBytes, StandardCharsets.UTF_8).replace("\uFEFF", "").strip();
        if (content.isEmpty()) {
            throw new InvalidResumeUploadException("Upload rejected: File contains only whitespace or is empty.");
        }
    }

    private boolean startsWithBytes(byte[] source, byte[] target) {
        if (source.length < target.length) return false;
        for (int i = 0; i < target.length; i++) {
            if (source[i] != target[i]) return false;
        }
        return true;
    }
}
