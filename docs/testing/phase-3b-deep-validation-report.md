# FairHire AI — Phase 3B Deep Validation Report

**Document:** `docs/testing/phase-3b-deep-validation-report.md`  
**Role:** Senior QA Engineer, Backend Engineer, Security Tester, Research-Software Auditor  
**Date:** October 4, 2026  
**Evaluation Scope:** Phase 3A (Resume Ingestion & Parsing) and Phase 3B (Deterministic Skill Extraction & Normalization)  
**Final Gate Status:** `FAIL — PHASE 3B NOT APPROVED FOR PHASE 4`  

---

## 1. Executive Summary

A comprehensive, adversarial deep validation of Phase 3A and Phase 3B was conducted across 22 rigorous test groups. The validation objective was to aggressively test token boundaries, taxonomy completeness, alias resolution, determinism, deduplication, edge-case file formats, lifecycle state transitions, database integrity, concurrency under stress, and security protections before permitting any Phase 4 (traditional ATS matching) implementation.

### Key Validation Outcomes:
1. **Determinism & Deduplication (Passed):** Extraction is 100% deterministic across 100 consecutive runs on complex multi-technology resumes (0 divergences). Deduplication correctly collapses redundant mentions and retains highest-confidence evidence.
2. **Security & Ingestion Integrity (Passed):** Windows executable binaries disguised as `.pdf` or `.docx`, path traversal payloads (`../../etc/passwd.pdf`), command injection filenames (`resume;rm -rf /;.pdf`), and oversized payloads (>10MB) are rejected with clear 400 Bad Request responses.
3. **High-Severity False Positive Bug (Failed - Bug #2):** The programming language `"Go"` is not constrained to case-sensitive matching, causing ordinary English verbs (`"go"`, `"go ahead"`, `"let it go"`, `"ready to go"`) to extract as candidate skills for the Google Go language.
4. **Migration Checksum Mismatch (Failed - Bug #4):** `V3__skill_taxonomy_and_extraction.sql` was modified after being applied to PostgreSQL, causing a Flyway validation failure (`11453495` vs `1441427275`) that breaks cold-boot application startup with Flyway validation enabled.
5. **False Negative on Sentence-Ending "C" (Failed - Bug #1):** Negative lookahead `(?![a-zA-Z0-9+#/._-])` contains `.`, causing `"C."` at sentence endings to fail extraction.
6. **API Contract Gap on Non-existent IDs (Failed - Bug #5):** `GET /api/v1/resumes/{id}/skills` returns `HTTP 200 OK` with an empty skill list for non-existent resume IDs instead of `HTTP 404 NOT FOUND`.
7. **Concurrency Conflict under Race (Failed - Bug #7):** Concurrent re-extraction requests on the same resume trigger `StaleObjectStateException` / HTTP 500 errors.

Per the Non-Negotiable Phase 4 Approval Rule, because unresolved high-severity bugs exist, Phase 3B is **NOT APPROVED** for Phase 4 until these findings are reviewed and addressed.

---

## 2. Validation Scope

The audit targeted all components implemented in Phase 3A and Phase 3B:
- Ingestion, validation, hashing, and storage (`ResumeValidator`, `StorageService`, `ResumeService`).
- PDF and DOCX parsers (`PdfTextExtractor`, `DocxTextExtractor`, `TextNormalizer`).
- Deterministic extraction and taxonomy (`DeterministicSkillExtractor`, `SkillExtractionService`, `Skill`, `ResumeSkill`).
- PostgreSQL 16 relational database schema, check constraints, foreign keys, and Flyway migrations (V1, V2, V3).
- REST APIs (`/api/v1/resumes`, `/api/v1/skills`, `/api/v1/resumes/{id}/skills`, `/api/v1/resumes/{id}/skills/extract`).

---

## 3. Environment

- **Host OS:** Windows 11 (PowerShell 7 / Command Line)
- **Container Runtime:** WSL2 (Ubuntu-24.04), Docker 27.x
- **Database:** PostgreSQL 16-alpine container (`fairhire-postgres` on port 5432)
- **Backend Service:** Spring Boot 3.3.4, Java 17 container (`fairhire-backend` on port 8088)
- **AI Service:** FastAPI, Python 3.11 container (`fairhire-ai-service` on port 5000)
- **Frontend Service:** React, Vite, Nginx container (`fairhire-frontend` on port 5173)

---

## 4. Baseline Test Results

- **Command:** `.\mvnw.cmd test`
- **Total Tests Run:** 56
- **Passed:** 55
- **Skipped:** 1 (`PostgresSchemaValidationTest` skipped when run on host Windows where direct Postgres port was not bound to host localhost)
- **Failures:** 0
- **Errors:** 0
- **Build Status:** `BUILD SUCCESS` (Duration: 24.38s)
- **WSL Postgres Schema Test:** `./mvnw test -Dtest=PostgresSchemaValidationTest` inside WSL failed with Flyway checksum mismatch (see Bug #4).

---

## 5. Canonical Taxonomy Results (Test Group 1)

Tested every canonical skill defined in the Phase 3B taxonomy in isolation:
- **Total Canonical Skills Tested:** 31
- **Passed:** 30
- **Failed:** 1
- **Failure Detail:**
  - `C`: Fails extraction when placed at the end of a sentence (`"Candidate has deep practical expertise in C."`). Extracted: `[]`. See Bug #1.
- **Unexpected Extraneous Skills:** 0.

---

## 6. Alias Exhaustiveness Results (Test Group 2)

Tested all 80 synonyms/aliases registered in the taxonomy:
- **Total Aliases Tested:** 80
- **Passed:** 79
- **Failed:** 1
- **Failure Detail:**
  - `Node.js` (alias `"Node.JS"`): Extracted via `CASE_INSENSITIVE_CANONICAL_MATCH` (conf=0.970) rather than `EXACT_ALIAS_MATCH` (0.950). This occurs because canonical rules take priority over alias rules, and `Node.js` case-insensitive pattern matches `Node.JS` first. (Expected behavior / redundant alias in taxonomy).
- **All other 79 aliases:** Successfully resolved to their exact canonical target with appropriate confidence tiers (`0.950` or `0.920`).

---

## 7. False Positive Results (Test Group 3 & 6)

Attacked token boundaries and natural English sentences:
- **Sub-token Collisions Prevented:**
  - `JavaScript` does NOT extract `Java` (Pass)
  - `Reaction` does NOT extract `React` (Pass)
  - `reactive` does NOT extract `React` (Pass)
  - `flAWS` and `draws` do NOT extract `AWS` (Pass)
  - `NoSQL`, `MySQL`, `PostgreSQL` do NOT extract `SQL` (Pass)
  - `Pythonic` does NOT extract `Python` (Pass)
  - `Spring 2024` does NOT extract `Spring Boot` (Pass)
- **Vulnerabilities Discovered:**
  - `"worker node in Kubernetes cluster"` $\to$ Falsely extracts `Node.js` (Bug #3).
  - `"cluster node and DOM node elements"` $\to$ Falsely extracts `Node.js` (Bug #3).
  - `"Always ready to go the extra mile"` $\to$ Falsely extracts `Go` (Bug #2).
  - `"Decided to let it go"` $\to$ Falsely extracts `Go` (Bug #2).
  - `"We must go ahead with the roadmap"` $\to$ Falsely extracts `Go` (Bug #2).

---

## 8. False Negative Results (Test Group 7)

Tested realistic resume layouts and structures:
- **Comma-Separated Skills Section:** 8/8 expected skills extracted (Pass).
- **Bullet-Point Skills Section:** 5/5 expected skills extracted (Pass).
- **Paragraph Work Experience:** 5/5 expected skills extracted (Pass).
- **Projects Section:** 5/5 expected skills extracted (Pass).
- **Education & Certifications:** 2/2 expected skills extracted (Pass).
- **ASCII Markdown Tables:** 10/10 expected skills extracted (Pass).
- **False Negative Bug:** Sentence-ending `C.` (Bug #1).

---

## 9. Boundary Testing Results (Test Group 4 & 5)

Tested symbol-rich technologies (`C`, `C++`, `C#`, `.NET`, `Node.js`):
- `(C++)`, `(C#)`, `(.NET)`, `(Node.js)`, `(C)` in parentheses: 100% extracted (Pass).
- Slash-separated `C/C++`: Extracts `C++` (Pass).
- Slashes `Java/JavaScript`: Extracts both `Java` and `JavaScript` (Pass).
- Hyphenated `Java-Developer`: Extracts `Java` (Pass).
- Concatenated `JavaDeveloper`, `C++Developer`, `C#Developer`: Correctly rejected (Pass).
- Case variations (`java`, `JAVA`, `JaVa`, `react`, `REACT`, `React.js`, `ReactJS`, `postgres`, `POSTGRESQL`): 22/22 normalized cleanly to canonical form (Pass).

---

## 10. Context / Evidence Integrity (Test Group 8)

- **Source Presence:** In 100% of extractions, `matched_text` was an exact substring of the original source text.
- **Snippet Relevance:** 100% of snippets contained the matched token and surrounding sentence.
- **Maximum Length Bound:** All snippets strictly conformed to $\le 280$ characters.
- **Edge Positions:** Snippets at document start, document end, and adjacent to punctuation extracted without index out-of-bounds errors.

---

## 11. Deduplication Results (Test Group 9)

- Single skill mentioned 1, 2, 10, and 100 times collapsed into exactly **1** canonical `ResumeSkill` record in the database.
- Multiple aliases + canonical name (e.g. `Java`, `Core Java`, `Java SE`, `Java programming`, `java 17`) collapsed into **1** `ResumeSkill` record.
- Dedup rule retained highest-confidence evidence (`EXACT_CANONICAL_MATCH: 1.000`).

---

## 12. Determinism Results (Test Group 10)

- **Test:** Multi-paragraph complex resume containing 18 distinct technologies extracted 100 consecutive times.
- **Execution Time:** 5.10 seconds total across 100 runs (~51 ms/run).
- **Divergences:** **0**.
- **Result:** 100% deterministic reproducibility across counts, skill IDs, confidence tiers, context snippets, and canonical ordering.

---

## 13. Idempotency Results (Test Group 11)

- **Test:** Repeated `POST /api/v1/resumes/{id}/skills/extract` sequentially 5 times on the same resume.
- **Counts:** `[18, 18, 18, 18, 18]`.
- **Database Status:** No duplicate rows generated; unique constraint `uq_resume_skills_resume_skill` fully respected.

---

## 14. Concurrency Results (Test Group 12)

- **Test:** Simultaneous extraction requests on the SAME resume entity.
- **5 Concurrent Requests:** 1 succeeded (200), 4 failed (500).
- **10 Concurrent Requests:** 2 succeeded (200), 8 failed (500).
- **20 Concurrent Requests:** 3 succeeded (200), 17 failed (500).
- **Root Cause:** Hibernate `StaleObjectStateException` during concurrent `deleteByResumeId` / `flush` execution without database row locking (`PESSIMISTIC_WRITE`).
- **Data Integrity:** **INTACT**. PostgreSQL transaction rollbacks prevented corrupted or duplicate rows; the final state consistently had exactly 18 skills.

---

## 15. Resume Pipeline Results (Test Group 13)

- **Test:** Ingested identical candidate text as `.txt`, `.docx`, and `.pdf`.
- **Results:**
  - TXT extracted: `['Docker', 'Java', 'Python']`
  - DOCX extracted: `['Docker', 'Java', 'Python']`
  - PDF extracted: `['Docker', 'Java', 'Python']`
- **Result:** `TXT skills == DOCX skills == PDF skills`. Full parity across formats.

---

## 16. Lifecycle Results (Test Group 14 & 15)

- Successful flow verified: `UPLOADED` $\to$ `PARSING` $\to$ `PARSED` $\to$ `SKILL_EXTRACTION` $\to$ `READY`.
- **Gap Discovered:** Whitespace-only files (`   \n\n  `) transition to `READY` with 0 skills rather than `FAILED` (Bug #6).

---

## 17. API Contract Results (Test Group 16)

- `GET /api/v1/skills` $\to$ HTTP 200 (Valid JSON array of skills).
- `GET /api/v1/skills/{id}` $\to$ HTTP 200 (Valid single skill) or HTTP 404 (Not found).
- `GET /api/v1/skills/invalid_abc` $\to$ HTTP 400 Bad Request.
- `POST /api/v1/resumes/{id}/skills/extract` $\to$ HTTP 404 for non-existent resume ID.
- `GET /api/v1/resumes/{id}/skills` $\to$ **HTTP 200 OK with empty array for non-existent ID** instead of HTTP 404 (Bug #5).
- **Information Leak Audit:** Zero stack traces, zero internal filesystem paths, zero raw SQL queries leaked in HTTP responses.

---

## 18. Database Integrity Results (Test Group 17)

- `resume_skills.resume_id` $\to$ `resumes.id ON DELETE CASCADE` (Verified).
- `resume_skills.skill_id` $\to$ `skills.id ON DELETE RESTRICT` (Verified).
- `uq_resume_skills_resume_skill` $\to$ `UNIQUE (resume_id, skill_id)` (Verified).
- `skills_name_key` $\to$ `UNIQUE (name)` (Verified).
- `ck_resumes_parsing_status` $\to$ Validates all 9 lifecycle states (Verified).
- `resume_skills_extraction_confidence_check` $\to$ Validates $0.0 \le \text{confidence} \le 1.0$ (Verified).

---

## 19. Transaction & Rollback Results (Test Group 18)

- If skill persistence fails midway, `@Transactional` triggers a database rollback.
- **Integrity:** Zero partial skills left in `resume_skills`.
- **Flaw:** Catching the exception and setting `ParsingStatus.SKILL_EXTRACTION_FAILED` within the same transaction that rolls back discards the status update (Bug #8).

---

## 20. Security Results (Test Group 20)

- Windows PE/MZ executables renamed to `.pdf` $\to$ Rejected (400 Bad Request).
- Windows PE/MZ executables renamed to `.docx` $\to$ Rejected (400 Bad Request).
- Path traversal filenames (`../../etc/passwd.pdf`, `..\..\calc.pdf`) $\to$ Rejected (400 Bad Request).
- Command injection characters (`resume;rm -rf /;.pdf`) $\to$ Rejected (400 Bad Request).
- Oversized uploads (12MB > 10MB limit) $\to$ Rejected (400 Bad Request).

---

## 21. Performance Results (Test Group 21)

- **1 KB Resume:** 470.47 ms
- **10 KB Resume:** 160.98 ms
- **50 KB Resume:** 481.41 ms
- **100 KB Resume:** 923.33 ms
- **500 KB Resume:** 5,273.93 ms (5.27s)
- **1 MB Resume:** 15,102.12 ms (15.10s)
- **5 MB Resume:** Request timed out (> 60s)
- **Analysis:** For realistic resume sizes ($\le 100$ KB), extraction runs in $< 1$s. On pathological multi-megabyte repeated text, regex scanning and linear span conflict checks exhibit $O(M^2)$ scaling.

---

## 22. Full Regression Results (Test Group 22)

- Phase 1, 2, 3A, 3B automated suites executed.
- Docker services verified:
  - `fairhire-postgres`: Healthy
  - `fairhire-ai-service`: Healthy
  - `fairhire-backend`: Healthy
  - `fairhire-frontend`: Healthy
- Cross-service communication: `fairhire-backend` successfully reaches `fairhire-ai-service:5000` inside the Docker network.

---

## 23. Bugs Found

### Bug B-01: False Negative on Single-Letter "C" at Sentence Endings
- **ID:** `B-01`
- **Severity:** `MEDIUM`
- **Component:** `DeterministicSkillExtractor.java` (`buildBoundaryRegex`)
- **Reproduction:** Ingest resume text: `"Candidate has deep practical expertise in C."`
- **Expected Behavior:** Skill `C` is extracted.
- **Actual Behavior:** 0 skills extracted.
- **Root Cause:** Negative lookahead `(?![a-zA-Z0-9+#/._-])` includes `.` to prevent `C.something`, but forbids standard sentence-ending periods.
- **Recommended Fix:** Change lookahead to `(?![a-zA-Z0-9+#/_\-]|\\.[a-zA-Z0-9])`.
- **Implementation Changed:** NO.

### Bug B-02: Programming Language "Go" Matches Common English Verb
- **ID:** `B-02`
- **Severity:** `HIGH`
- **Component:** `DeterministicSkillExtractor.java` (`addRuleIfValid`)
- **Reproduction:** Ingest text: `"Always ready to go the extra mile."` or `"Decided to let it go."`
- **Expected Behavior:** 0 skills extracted.
- **Actual Behavior:** Extracts `Go` (`PROGRAMMING_LANGUAGE`) with `CASE_INSENSITIVE_CANONICAL_MATCH`.
- **Root Cause:** `isSingleLetter = trimmed.length() == 1` only marks 1-character tokens as `caseSensitiveOnly`. `Go` has 2 characters, so it matches case-insensitively.
- **Recommended Fix:** Mark short common words (`Go`, `Rust`) as case-sensitive only (`caseSensitiveOnly = trimmed.length() <= 2 || isCommonEnglishWord(trimmed)`).
- **Implementation Changed:** NO.

### Bug B-03: Broad Alias "Node" Triggers False Positives on Infrastructure Terms
- **ID:** `B-03`
- **Severity:** `MEDIUM`
- **Component:** `skills` table seed & `V3__skill_taxonomy_and_extraction.sql`
- **Reproduction:** Ingest text: `"Architected high-throughput worker node in Kubernetes cluster."`
- **Expected Behavior:** Only `Kubernetes` extracted.
- **Actual Behavior:** Extracts `Node.js` (`FRAMEWORK`) with `EXACT_ALIAS_MATCH`.
- **Root Cause:** `"Node"` is registered as an alias for `"Node.js"`.
- **Recommended Fix:** Remove `"Node"` from `Node.js` synonyms.
- **Implementation Changed:** NO.

### Bug B-04: Flyway Migration Version 3 Checksum Mismatch
- **ID:** `B-04`
- **Severity:** `HIGH`
- **Component:** `V3__skill_taxonomy_and_extraction.sql`
- **Reproduction:** Run `PostgresSchemaValidationTest` against `fairhire_db`.
- **Expected Behavior:** Flyway validation passes.
- **Actual Behavior:** Flyway validation fails: `Applied: 11453495, Resolved locally: 1441427275`.
- **Root Cause:** `V3__skill_taxonomy_and_extraction.sql` was modified in place after being applied to the database.
- **Recommended Fix:** Revert in-place changes to V3 and place updates in `V4__...`, or run `flyway repair`.
- **Implementation Changed:** NO.

### Bug B-05: Non-existent Resume ID Returns HTTP 200 on Skills Endpoint
- **ID:** `B-05`
- **Severity:** `LOW`
- **Component:** `ResumeController.java` (`getExtractedSkills`)
- **Reproduction:** `GET /api/v1/resumes/999999/skills`
- **Expected Behavior:** HTTP 404 Not Found.
- **Actual Behavior:** HTTP 200 OK with `{"skills": []}`.
- **Root Cause:** Service queries `resumeSkillRepository.findByResumeId(999999)` directly without verifying resume existence.
- **Recommended Fix:** Check `resumeRepository.findById(resumeId)` and throw 404 if absent.
- **Implementation Changed:** NO.

### Bug B-06: Whitespace-Only Resume Ingestion Reaches "READY" Lifecycle State
- **ID:** `B-06`
- **Severity:** `MEDIUM`
- **Component:** `ResumeValidator.java` & `ResumeService.java`
- **Reproduction:** Upload `whitespace.txt` (`"   \n\n\t  "`).
- **Expected Behavior:** HTTP 400 Bad Request or `ParsingStatus.FAILED`.
- **Actual Behavior:** HTTP 201 Created with `ParsingStatus.READY`.
- **Root Cause:** File is not 0 bytes, so initial check passes; normalized text is empty, but neither validator nor service flags it as failed.
- **Recommended Fix:** Reject normalized blank text with `InvalidResumeUploadException` or set status to `FAILED`.
- **Implementation Changed:** NO.

### Bug B-07: Concurrency Race Condition During Re-Extraction
- **ID:** `B-07`
- **Severity:** `MEDIUM`
- **Component:** `SkillExtractionService.java`
- **Reproduction:** Send 5 concurrent re-extraction requests for the same resume.
- **Expected Behavior:** All succeed or queue gracefully.
- **Actual Behavior:** 1 succeeds, 4 fail with HTTP 500 (`StaleObjectStateException`).
- **Root Cause:** Concurrent `deleteByResumeId` / `flush` execution without row-level locking.
- **Recommended Fix:** Add `PESSIMISTIC_WRITE` lock on `Resume` during re-extraction.
- **Implementation Changed:** NO.

### Bug B-08: Transaction Rollback Discards `SKILL_EXTRACTION_FAILED` Status Update
- **ID:** `B-08`
- **Severity:** `MEDIUM`
- **Component:** `SkillExtractionService.java`
- **Reproduction:** Trigger an exception during skill persistence in `extractAndSaveSkills`.
- **Expected Behavior:** Resume status is updated to `SKILL_EXTRACTION_FAILED`.
- **Actual Behavior:** Status update is rolled back with the transaction.
- **Root Cause:** Exception re-thrown from `@Transactional` method rolls back writes made in the catch block.
- **Recommended Fix:** Update error status in a `REQUIRES_NEW` transaction.
- **Implementation Changed:** NO.

---

## 24. Accepted Limitations

1. **Vocabulary Boundary:** Skills outside the 31 canonical technologies are not recognized by design (deterministic baseline).
2. **Context-Free Ambiguity (Metallurgy vs. Tech):** Sentences like `"Handled rust corrosion on hardware"` extract `Rust` because rule-based matching has no semantic disambiguation.
3. **Regex Performance Scaling:** Documents $> 1$ MB take $> 15$ seconds due to linear span checks on repeated tokens. Acceptable for typical resume documents ($\le 100$ KB).

---

## 25. Specification Gaps

1. **Pre-existing Legacy Skills:** PostgreSQL retains 5 legacy skills (`Flask`, `Machine Learning`, `Pandas`, `NumPy`, `Data Science`) with category `"General"` from early prototypes.
2. **Synonym Discrepancy:** `DataInitializer.java` has a smaller synonym list for `Java` and `REST API` than `V3__skill_taxonomy_and_extraction.sql`.

---

## 26. Recommended Regression Tests

1. `testExtractSingleLetterCEndingSentence`: Verifies `C` is recognized when ending a sentence.
2. `testNoFalsePositiveForCommonEnglishVerbGo`: Verifies lowercase `"go"` does not extract `Go`.
3. `testNoFalsePositiveForClusterNode`: Verifies `"worker node"` does not extract `Node.js`.
4. `testNonExistentResumeSkillsReturns404`: Verifies `GET /resumes/999999/skills` returns 404.
5. `testWhitespaceOnlyResumeRejected`: Verifies empty/whitespace resumes fail ingestion.
6. `testConcurrentReExtractionResilience`: Verifies concurrent extractions do not throw 500.

---

## 27. Final Phase Gate

**GATE DECISION (INITIAL AUDIT):**

```text
FAIL — PHASE 3B NOT APPROVED FOR PHASE 4
```

### Rationale:
Phase 4 (traditional ATS keyword matching) depends directly on the skill extraction pipeline. High-severity bugs `B-02` (common English verb "go" contaminating candidate profiles) and `B-04` (Flyway migration checksum mismatch breaking cold-boot validation) must be resolved and approved before building Phase 4 scoring algorithms on top of Phase 3B.

---

## 28. Remediation Status (October 5, 2026)

All 8 identified defects (`B-01` through `B-08`) have been remediated, verified, and documented.
See: [`docs/testing/phase-3b-remediation-report.md`](file:///c:/Users/harhm/Downloads/FairHire_AI_Application/fairhire_ai/docs/testing/phase-3b-remediation-report.md)

**POST-REMEDIATION GATE ASSESSMENT:**
```text
PASS — READY FOR FINAL PHASE 3B REVALIDATION
```
