# FairHire AI — Resume Ingestion & Deterministic Text Extraction Pipeline

**Architecture Document — Phase 3A**  
**Version:** `v1.0-deterministic`  
**Status:** IMPLEMENTED & AUDITED  
**Date:** October 2026  

---

## 1. Overview & Objectives

Phase 3A establishes the ingestion, storage, validation, deterministic text extraction, and lifecycle tracking foundation for resumes in FairHire AI.

As a core requirement of this research-oriented recruitment screening framework:
- Text extraction must be **100% deterministic**, reproducible, and auditable.
- Resumes are stored as raw files on the filesystem through an abstracted storage interface, **never as database BLOBs**.
- Database records track cryptographic integrity (**SHA-256**), parser versioning, and parsing state transitions.
- Internal server file paths are **strictly quarantined** and never exposed across REST APIs.
- Downstream tasks (ATS scoring, skill extraction, semantic matching, bias detection, and blind screening) are decoupled and not executed during this phase.

```
+--------------------------------------------------------------------------------------------------+
|                                    RESUME INGESTION PIPELINE                                     |
+--------------------------------------------------------------------------------------------------+
|                                                                                                  |
|   Client Upload (multipart/form-data)                                                            |
|          │                                                                                       |
|          ▼                                                                                       |
|   [ResumeValidator] ──► Fails? ──► HTTP 400 Bad Request (Validation Exception)                   |
|     • Size bounds (1 byte to 10 MB)                                                             |
|     • Whitelist extension (.pdf, .docx, .txt)                                                    |
|     • MIME type check                                                                            |
|     • Magic byte signature verification (%PDF-, PK\x03\x04, UTF-8 text)                         |
|     • Executable & script header rejection (MZ, ELF, #!)                                         |
|     • Path traversal & shell injection sanitization                                              |
|          │                                                                                       |
|          ▼                                                                                       |
|   [SHA-256 Digest] ──► Duplicate in DB? ──► HTTP 409 Conflict (DuplicateResumeException)        |
|          │                                                                                       |
|          ▼                                                                                       |
|   [StorageService] ──► Persist to filesystem under opaque name: {sha256[:8]}_{uuid}.{ext}        |
|          │                                                                                       |
|          ▼                                                                                       |
|   [Resume Entity Created] ──► Status: UPLOADED                                                   |
|          │                                                                                       |
|          ▼                                                                                       |
|   [Text Extraction] ──► Status: PARSING                                                          |
|     • PDF  : Apache PDFBox 3.0.3 (Position-sorted reading order)                                 |
|     • DOCX : Apache POI 5.3.0 (Paragraphs & sequential table cells)                              |
|     • TXT  : Standard UTF-8 Reader                                                               |
|     • Empty/Scanned PDF? ──► Status: FAILED (OcrRequiredException)                               |
|          │                                                                                       |
|          ▼                                                                                       |
|   [TextNormalizer]                                                                               |
|     • Canonical newline conversion (\r\n / \r ──► \n)                                            |
|     • Zero-width & control character removal                                                     |
|     • Collapse excessive blank lines (>2 ──► 2)                                                  |
|     • Preserve case, punctuation, and semantic spacing                                           |
|          │                                                                                       |
|          ▼                                                                                       |
|   [Resume Entity Finalized] ──► Status: PARSED, parser_version='v1.0-deterministic'              |
|          │                                                                                       |
|          ▼                                                                                       |
|   HTTP 201 Created / 200 OK Response (ResumeUploadResponse DTO)                                  |
|                                                                                                  |
+--------------------------------------------------------------------------------------------------+
```

---

## 2. Supported Formats & Extraction Engines

| Format | Library & Version | Extraction Method | Edge-Case Handling |
| :--- | :--- | :--- | :--- |
| **PDF** | Apache PDFBox `3.0.3` | `PDFTextStripper` with `setSortByPosition(true)` | Handles multi-column layouts; detects scanned/image-only PDFs lacking extractable text. |
| **DOCX** | Apache POI `5.3.0` (`poi-ooxml`) | `XWPFDocument` element traversal | Extracts paragraphs and table cell contents in top-to-bottom document order. |
| **TXT** | Java NIO / Apache Commons IO | UTF-8 charset decoding | Handles varying line endings and strips BOM headers safely. |

### Deterministic Text Extraction Principles
1. **No Heuristic Reordering:** Reading order in PDF is established strictly through positional coordinates (`PDFTextStripper.setSortByPosition(true)`).
2. **Table Preservation:** In DOCX documents, table text is interleaved sequentially with paragraphs in the exact structural order of the XML DOM.
3. **No LLM/Statistical Alteration:** Extraction is completely deterministic; identical document bytes yield byte-for-byte identical extracted text across runs and environments.

---

## 3. Scanned & Image-Only PDF Handling

FairHire AI does not bundle OCR libraries (e.g. Tesseract) in Phase 3A to avoid unvetted non-deterministic dependencies.

When a PDF file:
1. Contains valid PDF structures and fonts but renders less than 10 non-whitespace characters across all pages, OR
2. Consists exclusively of scanned embedded raster images (`/Image` XObjects) without an underlying text layer,

The pipeline triggers an `OcrRequiredException`. The resume record is retained in the database with:
- `parsing_status = 'FAILED'`
- `parsing_error = 'OCR required for image-only or scanned PDF document'`

This gives recruiters clear visibility that the candidate resume is a scanned image requiring OCR preprocessing rather than silently treating it as an empty document.

---

## 4. Multi-Layer Upload Security Architecture

To prevent arbitrary code execution, zip bombs, and filesystem attacks, all uploads must pass the `ResumeValidator` guard before disk writes or extraction occur.

### 4.1. File Size Verification
- Max file size: **10 MB** (`10,485,760 bytes`).
- Min file size: **1 byte** (empty files rejected with `400 Bad Request`).

### 4.2. Whitelist File Extensions & MIME Types
- Only `.pdf`, `.docx`, and `.txt` extensions are accepted.
- Content types are checked against canonical MIME categories:
  - `application/pdf`
  - `application/vnd.openxmlformats-officedocument.wordprocessingml.document`
  - `application/msword`
  - `text/plain`

### 4.3. Magic Byte Signature Verification
Filename extensions are inherently untrustworthy. Every upload is inspected at byte offset 0:
- **PDF:** Must begin with `%PDF-` (`0x25 0x50 0x44 0x46 0x2D`).
- **DOCX:** Must begin with PK zip header `PK\x03\x04` (`0x50 0x4B 0x03 0x04`).
- **Executables Rejected:** Explicitly blocks Windows PE (`MZ`, `0x4D 0x5A`), Linux ELF (`\x7FELF`, `0x7F 0x45 0x4C 0x46`), and Unix script shebang (`#!`, `0x23 0x21`).

### 4.4. Path Traversal & Shell Injection Sanitization
- Input filenames are stripped of directories using `Paths.get(rawFilename).getFileName().toString()`.
- Filenames containing path traversal patterns (`..`, `/`, `\`) or command-line shell tokens (`;`, `|`, `&`, `` ` ``, `$`) are rejected immediately.
- Files are stored under **opaque generated UUID filenames** (e.g. `13abc353_6b82-414c-9f6b-7341e42f9da3.pdf`), preventing directory overwrites or filesystem clobbering.

---

## 5. Storage Architecture & Abstraction

### 5.1. StorageService Interface
A pluggable abstraction decouples the application from physical disk implementations:
```java
public interface StorageService {
    String store(MultipartFile file, String subDirectory);
    InputStream load(String storagePath);
    void delete(String storagePath);
    boolean exists(String storagePath);
}
```

### 5.2. Local FileSystem Storage (`FileSystemStorageService`)
- Base storage directory: configured via `app.storage.base-path` (default: `./uploads/resumes`).
- Files are saved with strict non-executable permissions.
- Paths are resolved against the canonical root directory with path escape validation (`normalizedPath.startsWith(rootPath)`).

### 5.3. Public API Path Privacy
Internal storage paths (e.g. `/app/uploads/resumes/13abc353_...pdf` or `C:\uploads\...`) are **never returned in API responses**. The REST API exposes only:
- `resumeId`
- `fileName` (original display name)
- `fileType` (`PDF`, `DOCX`, `TXT`)
- `fileSizeBytes`
- `fileHashSha256`
- `parsingStatus`
- `parserVersion`
- `rawText` (and derived char/word metrics)

---

## 6. Text Normalization Pipeline

Raw extracted text from various engines exhibits inconsistencies in newline formats, Unicode whitespace, and control codes. The `TextNormalizer` applies deterministic transformations:

1. **Newline Canonicalization:** Translates `\r\n` (Windows CRLF) and `\r` (Classic Mac) to standard Unix `\n`.
2. **Zero-Width & Control Stripping:** Removes zero-width spaces (`\u200B`), zero-width joiners (`\u200C`, `\u200D`), byte order marks (`\uFEFF`), and non-printable ASCII control codes (`\x00-\x08`, `\x0B-\x0C`, `\x0E-\x1F`).
3. **Unicode Space Normalization:** Converts non-breaking spaces (`\u00A0`), en-spaces (`\u2002`), and em-spaces (`\u2003`) to standard ASCII spaces (`\u0020`).
4. **Blank Line Collapsing:** Collapses streaks of 3 or more consecutive newlines down to 2 newlines (preserving clear paragraph breaks without runaway vertical spacing).
5. **Casing & Punctuation Preservation:** Casing (capitalization), punctuation, technical symbols (`C++`, `C#`, `.NET`), and numbers are strictly preserved for downstream NLP and skill matching.

---

## 7. Resume Lifecycle State Machine

The resume record transitions through explicit lifecycle states:

```
           [Upload Request Received]
                      │
                      ▼
                 ┌──────────┐
                 │ UPLOADED │
                 └────┬─────┘
                      │
                      ▼ (Begin Extraction)
                 ┌──────────┐
                 │ PARSING  │
                 └────┬─────┘
                      │
           ───────────┴───────────
          │                       │
 (Success)│                       │ (Error / OCR Needed)
          ▼                       ▼
    ┌──────────┐            ┌──────────┐
    │  PARSED  │            │  FAILED  │
    └──────────┘            └──────────┘
```

- **`UPLOADED`**: File validated, hashed, stored on disk, and DB entity created.
- **`PARSING`**: Text extraction engine currently processing file stream.
- **`PARSED`**: Text extracted, normalized, and saved to `raw_text` along with word/char counts and `parser_version = 'v1.0-deterministic'`.
- **`FAILED`**: Extraction failed (scanned PDF, corrupt archive, unsupported encoding). The `parsing_error` column records the specific diagnostic message.

---

## 8. Database Schema (Flyway V2)

The `resumes` table schema was updated in `V2__resume_ingestion_lifecycle.sql` under the forward-only migration policy:

```sql
-- Updated parsing_status check constraint
ALTER TABLE resumes DROP CONSTRAINT IF EXISTS ck_resumes_parsing_status;
ALTER TABLE resumes ADD CONSTRAINT ck_resumes_parsing_status 
    CHECK (parsing_status IN ('UPLOADED', 'PARSING', 'PARSED', 'FAILED', 'PENDING', 'COMPLETED'));

-- Parser version and error tracking
ALTER TABLE resumes ADD COLUMN IF NOT EXISTS parser_version VARCHAR(50) NOT NULL DEFAULT 'v1.0-deterministic';
ALTER TABLE resumes ADD COLUMN IF NOT EXISTS parsing_error TEXT;
ALTER TABLE resumes ADD COLUMN IF NOT EXISTS file_size_bytes BIGINT;

-- Status index for processing queries
CREATE INDEX IF NOT EXISTS idx_resumes_parsing_status ON resumes (parsing_status);
```

---

## 9. Verification & Audit Results

### 9.1. Automated Test Suite
- Total tests executed: **42**
- Failures: **0**
- Errors: **0**
- Test categories covered:
  - `PdfTextExtractorTest`: Single/multi-page order, scanned PDF detection, malformed PDF handling.
  - `DocxTextExtractorTest`: Standard text, table extraction order, malformed DOCX handling.
  - `TextNormalizerTest`: CRLF normalization, zero-width cleanup, blank line collapsing, technical token preservation.
  - `ResumeValidatorTest`: File size, extension whitelist, MIME checks, magic bytes (`%PDF-`, `PK`), executable rejection (`MZ`), path traversal.
  - `ResumeIngestionIntegrationTest`: End-to-end PDF/DOCX ingestion, duplicate rejection (`409 Conflict`), SHA-256 persistence.
  - `PostgresSchemaValidationTest`: Live PostgreSQL 16 Flyway V1 and V2 schema validation with Hibernate `ddl-auto=validate`.

### 9.2. Live Container Verification (Docker Compose)
- Live container `fairhire-backend` verified running on port 8088.
- Valid PDF upload tested via `curl`: returned `201 Created` / `200 OK` with full extracted text and SHA-256 hash.
- Duplicate PDF upload tested via `curl`: returned `409 Conflict` with `Duplicate resume detected`.
- Valid DOCX upload tested via `curl`: returned `201 Created` / `200 OK` with full POI extracted text.
- Spoofed fake PDF (`fake.pdf`) tested via `curl`: returned `400 Bad Request` with magic byte error message.
- Malicious executable (`malicious.exe`) tested via `curl`: returned `400 Bad Request` with unsupported extension error message.
- System health `/api/v1/health/system` verified: PostgreSQL 16.13 UP, AI Service UP, Backend UP.
