# FairHire AI — Phase 4C Implementation & QA Remediation Report
**Match Explainability & Auditable Match Results**

**Status:** Complete (Remediated)  
**Date:** 2026-10-07  
**Implementation Phase:** Phase 4C (Explainability & Auditable Match Results)  
**Verification Result:** PASS (0 failures, 0 errors, 1 skipped)

---

## 1. Executive Summary & Remediation Overview
Following the Phase 4C QA review, this report documents the remediation of both flagged QA blockers:
1. **Blocker 1 Resolved — Strict Fail-Closed Authorization Boundary:**
   - Eliminated any optionality or silent default substitution where caller identity could fall back to `job.department`.
   - `ResourceAuthorizationService.validateJobAccess(job, callerDepartment)` now strictly enforces that caller department must be explicitly supplied, non-blank, and match the target job's department.
   - Missing or blank `X-Department` headers on `GET /api/v1/jobs/{jobId}/matches` and `GET /api/v1/jobs/{jobId}/matches/{resumeId}` fail closed immediately with HTTP 403 `UNAUTHORIZED_RESOURCE_ACCESS`.
   - Added dedicated tests for Oracles E10-A, E10-B, E10-C, and E10-D verifying that callers cannot access matches without independent department authentication and cannot derive authorization from the protected job's own department.
2. **Blocker 2 Resolved — Transparent & Accurate Verification Reporting:**
   - Clearly documented the 1 skipped test (`PostgresSchemaValidationTest`) under the local/H2 test profile.
   - Stated honestly that the Docker daemon was not running on the local host machine, so PostgreSQL container execution was not run live; no PostgreSQL PASS is fabricated.
   - Statically and migrationally confirmed that Flyway migrations V1–V5 remain unchanged, V3 checksum is exactly `11453495` (verified via `Phase4PersistenceTest`), and NO V6 migration was created.

---

## 2. Files Created
1. `backend/src/test/java/com/fairhire/services/matching/MatchExplanationIntegrationTest.java`: Comprehensive Spring Boot integration test suite covering Oracles E1 through E14 (17 test methods total):
   - **Oracle E1:** Perfect Match Explanation (all required and preferred matched, correct counts, coverages, scores, weights, contributions, algorithm metadata, and sanitized evidence)
   - **Oracle E2:** Missing Required Skill (unmatched skill has `matched = false`, `candidateConfidence = null`, and `evidence = null`)
   - **Oracle E3:** Mixed Required / Preferred Separation (clean separation into distinct sections)
   - **Oracle E4:** Overlap Precedence (skill present in both categories appears strictly once under REQUIRED)
   - **Oracle E5:** PII Safety (emails, phone numbers, URLs, and street addresses are scrubbed from evidence snippets)
   - **Oracle E6:** No Raw Resume Access (evidence is served strictly from persisted `MatchSkillDetail`; no raw resume loading or reparsing occurs)
   - **Oracle E7:** Stale Result Visibility (`is_stale = true` and `status = "STALE"` exposed; no recalculation on GET)
   - **Oracle E8:** Historical Explanation Integrity (modifications to active job skills or resume skills do not alter historical explanation)
   - **Oracle E9:** Legacy SEMANTIC Result Handling (legacy semantic results are served without fabricating synthetic canonical skill details)
   - **Oracle E10-A:** Authorized Department (Caller department matches Job.department -> 200 OK)
   - **Oracle E10-B:** Wrong Department (Caller department differs from Job.department -> 403 `UNAUTHORIZED_RESOURCE_ACCESS`)
   - **Oracle E10-C:** Missing Caller Authorization (null/blank caller department fails closed -> 403 `UNAUTHORIZED_RESOURCE_ACCESS`)
   - **Oracle E10-D:** Cannot Derive Authorization from Protected Job (Missing caller cannot fall back to `job.department` -> fails closed)
   - **Oracle E11:** Not Found Error Handling (unknown job, resume, or match returns 404 `RESOURCE_NOT_FOUND`)
   - **Oracle E12:** Deterministic Ordering (skills are ordered deterministically by `jobSkillId ASC` across repeated GET calls)
   - **Oracle E13:** Read-Only Invariance (calling explanation endpoint performs zero mutations on database state)
   - **Oracle E14:** Score Authority (returned score reflects persisted `MatchResult`, not recomputed live values)
2. `docs/qa/phase-4c-implementation-report.md`: This comprehensive implementation and verification report.

---

## 3. Files Modified
1. `backend/src/main/java/com/fairhire/services/matching/ResourceAuthorizationService.java`:
   - Updated `validateJobAccess(Job job, String callerDepartment)` to enforce strict fail-closed semantics: callerDepartment must be explicitly supplied, non-blank, and match `job.getDepartment()`.
   - If callerDepartment is null or blank, throws `UnauthorizedResourceAccessException` immediately.
2. `backend/src/main/java/com/fairhire/dto/MatchSkillDetailDto.java`:
   - Added explainability accessors and JSON aliases (`matched`, `matched_text` / `matchedText`, `context_snippet` / `contextSnippet`) while preserving backward-compatible properties (`is_matched`, `candidate_matched_text`, `candidate_context_snippet`).
3. `backend/src/main/java/com/fairhire/dto/MatchDetailResponseDto.java`:
   - Added explainability and audit metadata fields: `status` (`"ACTIVE"` / `"STALE"`), `required_weight` (`0.80`), `preferred_weight` (`0.20`), `required_contribution`, `preferred_contribution`.
   - Added JSON property aliases (`matchResultId`, `jobId`, `resumeId`, `overallScore`, `isStale`, etc.) for seamless camelCase and snake_case API interoperability.
4. `backend/src/main/java/com/fairhire/services/matching/SkillCoverageMatchingService.java`:
   - `getMatchDetail(Long jobId, Long resumeId, String callerDepartment)` and `getJobMatches(Long jobId, String callerDepartment)` strictly call `authorizationService.validateJobAccess(job, callerDepartment)`, failing closed if caller department is missing or mismatched.
   - Overloads `getMatchDetail(Long jobId, Long resumeId)` and `getJobMatches(Long jobId)` pass `null` as caller department, ensuring fail-closed behavior and preventing derivation of authorization from `job.getDepartment()`.
   - `matchJob` validates caller access whenever `callerDepartment` is provided, preserving Phase 4B programmatic compatibility.
   - Decomposes exact 80/20 weights and contributions via `BigDecimal` with `RoundingMode.HALF_UP` (Scale 2).
   - Enforces overlap precedence (REQUIRED takes precedence over PREFERRED; each skill appears at most once).
   - Enforces deterministic ordering by `jobSkillId ASC`.
   - Scrubs evidence snippets via `DeterministicPiiScrubber`.
   - Handles legacy SEMANTIC match results gracefully without fabricating canonical skill details.
5. `backend/src/main/java/com/fairhire/controllers/JobMatchingController.java`:
   - `GET /api/v1/jobs/{jobId}/matches` and `GET /api/v1/jobs/{jobId}/matches/{resumeId}` receive `@RequestHeader(value = "X-Department", required = false) String headerDepartment` and pass it to the service layer.
   - Catches `UnauthorizedResourceAccessException` and returns HTTP 403 `UNAUTHORIZED_RESOURCE_ACCESS`.
6. `backend/src/test/java/com/fairhire/controllers/JobMatchingControllerTest.java`:
   - Expanded to 14 MockMvc tests covering authorized, missing, blank, and wrong department headers for both GET endpoints, plus 404 not found and 422 validations.
7. `backend/src/test/java/com/fairhire/services/matching/SkillCoverageMatchingIntegrationTest.java`:
   - Updated `getMatchDetail` calls to pass authorized department `"Engineering"`.

---

## 4. Deterministic Match Explanation Model
The explanation model decomposes the persisted match result into auditable facts:

```json
{
  "match_id": 123,
  "job_id": 10,
  "resume_id": 50,
  "candidate_id": 99,
  "matching_method": "CANONICAL_SKILL_COVERAGE",
  "algorithm_version": "deterministic-v1",
  "overall_score": 73.33,
  "required_skill_coverage": 0.6667,
  "preferred_skill_coverage": 1.0000,
  "required_skills_total": 3,
  "required_skills_matched": 2,
  "preferred_skills_total": 2,
  "preferred_skills_matched": 2,
  "required_weight": 0.80,
  "preferred_weight": 0.20,
  "required_contribution": 53.34,
  "preferred_contribution": 20.00,
  "status": "ACTIVE",
  "is_stale": false,
  "scored_at": "2026-10-06T12:00:00Z",
  "required_skills": [
    {
      "id": 1001,
      "job_skill_id": 201,
      "skill_id": 1,
      "skill_name": "Java",
      "necessity": "REQUIRED",
      "is_matched": true,
      "matched": true,
      "candidate_confidence": 0.920,
      "matched_text": "Java",
      "context_snippet": "Developed high-throughput backend services using Java..."
    },
    {
      "id": 1002,
      "job_skill_id": 202,
      "skill_id": 2,
      "skill_name": "Docker",
      "necessity": "REQUIRED",
      "is_matched": false,
      "matched": false,
      "candidate_confidence": null,
      "matched_text": null,
      "context_snippet": null
    }
  ],
  "preferred_skills": [
    {
      "id": 1003,
      "job_skill_id": 203,
      "skill_id": 4,
      "skill_name": "AWS",
      "necessity": "PREFERRED",
      "is_matched": true,
      "matched": true,
      "candidate_confidence": 0.850,
      "matched_text": "AWS",
      "context_snippet": "Deployed serverless functions on AWS Lambda..."
    }
  ]
}
```

---

## 5. Resource Authorization & Department Boundary (Blocker 1 Remediation)

### Fail-Closed Authorization Policy
Authorization is enforced at both the Controller and Service boundaries:
- **Case A — Valid Authorized Caller (`callerDepartment.equalsIgnoreCase(job.getDepartment())`):**
  - Caller provides `X-Department: Engineering` for a job in department `Engineering`.
  - Result: HTTP 200 OK.
- **Case B — Cross-Department Caller (`!callerDepartment.equalsIgnoreCase(job.getDepartment())`):**
  - Caller provides `X-Department: Marketing` for a job in department `Engineering`.
  - Result: HTTP 403 `UNAUTHORIZED_RESOURCE_ACCESS`.
- **Case C — Missing Caller Authorization Context (`callerDepartment == null || callerDepartment.isBlank()`):**
  - Caller omits `X-Department` or supplies a blank header (`"   "`).
  - Result: HTTP 403 `UNAUTHORIZED_RESOURCE_ACCESS`.
- **Case D — Non-Derivation Invariant:**
  - The endpoint and service NEVER derive authorization from `job.getDepartment()`.
  - Missing caller identity cannot default to the target resource's department.
  - Invariant enforced: `resource.department != caller.department` when caller identity is omitted.

### Both Endpoints Verified
- `GET /api/v1/jobs/{jobId}/matches`: Fails closed with HTTP 403 if `X-Department` is missing, blank, or mismatched.
- `GET /api/v1/jobs/{jobId}/matches/{resumeId}`: Fails closed with HTTP 403 if `X-Department` is missing, blank, or mismatched.

---

## 6. MatchResult & MatchSkillDetail Authority
- `MatchResult` is the sole source of truth for:
  - `overallScore`
  - `requiredSkillCoverage` and `preferredSkillCoverage`
  - `requiredSkillsMatched` / `requiredSkillsTotal`
  - `preferredSkillsMatched` / `preferredSkillsTotal`
  - `algorithmVersion`, `matchingMethod`, `isStale`, and `scoredAt`
- `MatchSkillDetail` is the sole source of truth for evaluated requirements:
  - `is_matched`
  - `candidate_confidence`
  - `candidate_matched_text`
  - `candidate_context_snippet`
- If `is_matched = false`, confidence and evidence are strictly `null`.
- Raw resume text is NEVER loaded or reparsed to construct explanations.

---

## 7. PII Handling & Evidence Redaction
1. **Pre-Persistence Scrubbing:** Raw candidate evidence was scrubbed via `DeterministicPiiScrubber` before insertion into `match_skill_details`.
2. **Pre-Exposure Scrubbing:** Evidence retrieved from `match_skill_details` is scrubbed again through `DeterministicPiiScrubber`:
   - Emails redacted to `[REDACTED_EMAIL]`
   - Phone numbers redacted to `[REDACTED_PHONE]`
   - URLs redacted to `[REDACTED_URL]`
   - Addresses redacted to `[REDACTED_ADDRESS]`
3. **Length Bounding:** `matched_text` capped at 100 characters; `context_snippet` capped at 300 characters.
4. **Candidate Entity Isolation:** The Candidate entity is never queried or exposed in match explanations.

---

## 8. Stale-Result Behavior & Read-Only Invariance
- If `is_stale = true` on `MatchResult`, the response returns:
  ```json
  "is_stale": true,
  "status": "STALE"
  ```
- GET requests NEVER trigger score recalculation or change `is_stale`.
- The GET endpoint is `@Transactional(readOnly = true)`: zero database mutations occur on read.

---

## 9. Historical-Result Behavior
- Historical match results remain explainable using their persisted `MatchResult` and `MatchSkillDetail` records.
- Mutations to active `job_skills` or candidate `resume_skills` do not alter historical breakdown or scores.

---

## 10. Legacy Semantic Result Handling
- For historical `SEMANTIC` match results:
  - Exposes `algorithm_version = model_version` (or `"SEMANTIC"`), `matching_method = "SEMANTIC"`, and `overall_score = semantic_score * 100`.
  - Does NOT fabricate synthetic canonical skill details.
  - Returns empty lists for `required_skills` and `preferred_skills`.
  - Historical `semantic_score` and `model_version` remain untouched.

---

## 11. Deterministic Ordering
- Both `required_skills` and `preferred_skills` lists are ordered deterministically by `jobSkillId ASC` (`matchSkillDetailRepository.findByMatchResultIdOrderByJobSkillIdAsc` + in-memory sort).
- Repeated GET requests yield identical ordering.

---

## 12. Test Oracles Verification (Oracles E1 through E14)

| Oracle | Description | Verification Method | Result |
|---|---|---|---|
| **E1** | Perfect Match Explanation | `MatchExplanationIntegrationTest.oracleE1_perfectMatchExplanation` | **PASS** |
| **E2** | Missing Required Skill (`matched = false`, `evidence = null`) | `MatchExplanationIntegrationTest.oracleE2_missingRequiredSkill` | **PASS** |
| **E3** | Mixed Required / Preferred Separation | `MatchExplanationIntegrationTest.oracleE3_mixedRequiredPreferredSeparation` | **PASS** |
| **E4** | Overlap Precedence (Skill appears only under REQUIRED) | `MatchExplanationIntegrationTest.oracleE4_overlapPrecedence` | **PASS** |
| **E5** | PII Safety (Evidence redaction of email, phone, URL, address) | `MatchExplanationIntegrationTest.oracleE5_piiSafety` | **PASS** |
| **E6** | No Raw Resume Access (Relies strictly on MatchSkillDetail) | `MatchExplanationIntegrationTest.oracleE6_noRawResumeAccess` | **PASS** |
| **E7** | Stale Result Visibility (`is_stale = true`, `status = STALE`) | `MatchExplanationIntegrationTest.oracleE7_staleResultVisibility` | **PASS** |
| **E8** | Historical Explanation Integrity (Resilient to skill mutations) | `MatchExplanationIntegrationTest.oracleE8_historicalExplanationIntegrity` | **PASS** |
| **E9** | Legacy SEMANTIC Result Handling (No fabricated details) | `MatchExplanationIntegrationTest.oracleE9_legacySemanticResult` | **PASS** |
| **E10-A** | Authorized Department (200 OK when callerDept == jobDept) | `MatchExplanationIntegrationTest.testOracleE10A_AuthorizedDepartment` | **PASS** |
| **E10-B** | Wrong Department (403 when callerDept != jobDept) | `MatchExplanationIntegrationTest.testOracleE10B_WrongDepartment` | **PASS** |
| **E10-C** | Missing Caller Authorization (403 when callerDept is null/blank) | `MatchExplanationIntegrationTest.testOracleE10C_MissingCallerAuthorization` | **PASS** |
| **E10-D** | Cannot Derive Authorization from Job (fails closed without fallback) | `MatchExplanationIntegrationTest.testOracleE10D_CannotDeriveAuthorizationFromProtectedJob` | **PASS** |
| **E11** | Not Found Error Handling (404 on missing job/resume/match) | `MatchExplanationIntegrationTest.oracleE11_notFoundErrorHandling` | **PASS** |
| **E12** | Deterministic Ordering (`jobSkillId ASC` across repeated calls) | `MatchExplanationIntegrationTest.oracleE12_deterministicOrdering` | **PASS** |
| **E13** | Read-Only Invariance (Zero DB mutations on GET) | `MatchExplanationIntegrationTest.oracleE13_readOnlyInvariance` | **PASS** |
| **E14** | Score Authority (Returns persisted score, not recomputed) | `MatchExplanationIntegrationTest.oracleE14_scoreAuthority` | **PASS** |

---

## 13. Full Maven Test Suite Results
Full test suite execution command:
```bash
./mvnw.cmd test
```

### Test Execution Summary
- **Total Tests Run:** 152
- **Passed:** 151
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 1 (`PostgresSchemaValidationTest` when running under H2 test profile)
- **Phase 4B Baseline:** 129 tests
- **Phase 4C Tests Added:** +23 tests (17 in `MatchExplanationIntegrationTest` + 6 added to `JobMatchingControllerTest`)

### Detailed Suite Breakdown
- `JobMatchingControllerTest`: 14 tests passing
- `MatchExplanationIntegrationTest`: 17 tests passing
- `SkillCoverageMatchingIntegrationTest`: 19 tests passing
- `DeterministicScoringEngineTest`: 10 tests passing
- `DeterministicPiiScrubberTest`: 8 tests passing
- `Phase4PersistenceTest`: 9 tests passing
- `SkillExtractionIntegrationTest`: 8 tests passing
- `DeterministicSkillExtractorTest`: 19 tests passing
- `SkillPersistenceIntegrationTest`: 6 tests passing
- `JobServiceTest`: 14 tests passing
- `ResumeUploadIntegrationTest`: 10 tests passing
- `PostgresSchemaValidationTest`: 1 test skipped (H2 test profile)
- `FairHireAiApplicationTests`: 1 test passing
- `SkillExtractionPerformanceBenchmarkTest`: 1 test passing
- Plus all remaining Phase 3A/3B domain and parser tests.

---

## 14. Database Verification & PostgreSQL Status (Blocker 2 Remediation)
- **PostgreSQL Environment Status:**
  - Docker daemon was not running on the host system (`failed to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine`).
  - As instructed, this is stated honestly: live container execution (`docker exec fairhire-postgres psql ...`) was **NOT** independently executed. No PostgreSQL PASS is fabricated.
- **Migration & Schema History Verification:**
  - Flyway migrations present in `src/main/resources/db/migration/`:
    - `V1__init_schema.sql` (Unmodified)
    - `V2__seed_taxonomy_and_synonyms.sql` (Unmodified)
    - `V3__hardening_and_integrity_constraints.sql` (Unmodified, Checksum: `11453495`)
    - `V4__phase_3b_resilience_and_indexes.sql` (Unmodified)
    - `V5__phase_4_matching_engine.sql` (Unmodified from Phase 4A)
  - **V3 Checksum Verified:** `Phase4PersistenceTest.testFlywayMigrationHistory()` verified that V3 checksum is exactly `11453495`.
  - **No V6 migration created:** Phase 4C operates exclusively on the approved Phase 4A schema.

---

## 15. Git Diff Summary
- Modified files:
  - `backend/src/main/java/com/fairhire/services/matching/ResourceAuthorizationService.java`
  - `backend/src/main/java/com/fairhire/services/matching/SkillCoverageMatchingService.java`
  - `backend/src/main/java/com/fairhire/controllers/JobMatchingController.java`
  - `backend/src/main/java/com/fairhire/dto/MatchSkillDetailDto.java`
  - `backend/src/main/java/com/fairhire/dto/MatchDetailResponseDto.java`
  - `backend/src/test/java/com/fairhire/controllers/JobMatchingControllerTest.java`
  - `backend/src/test/java/com/fairhire/services/matching/SkillCoverageMatchingIntegrationTest.java`
  - `backend/src/test/java/com/fairhire/services/matching/MatchExplanationIntegrationTest.java`
  - `docs/qa/phase-4c-implementation-report.md`
- Unmodified files:
  - All Flyway migrations V1–V5. V3 checksum intact (`11453495`).
  - `DeterministicScoringEngine.java` (scoring mathematics intact).

---

## 16. Forbidden Features Explicitly NOT Implemented
In strict compliance with the Phase 4C mandate, the following were NOT implemented:
- ❌ No machine learning / AI models / LLMs / embeddings / SBERT.
- ❌ No fuzzy matching / TF-IDF / Jaccard / cosine similarity.
- ❌ No frontend UI / recruiter dashboard.
- ❌ No automatic hiring or rejection decisions.
- ❌ No demographic inference or bias prediction.
- ❌ No candidate recommendation engines.
- ❌ No resume rewriting or candidate coaching.
- ❌ No background worker tasks or asynchronous queues.
- ❌ No Phase 5 features.

---

## 17. Conclusion
Both Phase 4C QA blockers have been remediated:
1. Authorization fails closed across both GET endpoints and the service layer when caller department context is missing, blank, or mismatched, with zero derivation from `job.department`.
2. All test counts and skipped status are accurately documented without fabrication.
Every match explanation is strictly derived from persisted, auditable facts in `MatchResult` and `MatchSkillDetail`.

**Final Status:** PHASE 4C REMEDIATION COMPLETE — AWAITING QA RE-GATE
