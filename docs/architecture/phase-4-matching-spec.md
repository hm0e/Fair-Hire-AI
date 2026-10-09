# FairHire AI — Phase 4 Matching & Scoring Specification
## Deterministic Job–Candidate Compatibility & Explainability Engine

**Document ID:** `SPEC-PHASE-4-MATCHING-001`  
**Version:** `1.3.0-FINAL-CONSISTENCY`  
**Status:** `ARCHITECTURE DESIGN COMPLETE — AWAITING APPROVAL`  
**Author:** Phase 4 Architecture & Design Team  
**Date:** 2026-10-06  
**Depends On:** Phase 3A (Resume Parser & Persistence), Phase 3B (Deterministic Skill Taxonomy & Extraction — Checksum `11453495`)

---

## 1. Executive Summary & Purpose

This document provides the mathematical, algorithmic, and data-contract specification for the FairHire AI **Phase 4 Job–Candidate Matching Engine**.

The Phase 4 Matching Engine is governed by strict ethical, legal, and operational mandates:
1. **Zero Opaque Machine Learning**: No Sentence-BERT (SBERT), TF-IDF, Word2Vec, LLM scoring, or latent semantic embeddings. No dependency on `http://localhost:8000/embed`.
2. **Strict Determinism**: Matching is an invariant, pure mathematical function. Identical candidate skills and job requirements evaluated under identical taxonomy versions yield identical component scores, coverage ratios, matched/missing skill sets, and explanation trees across all runs (excluding runtime-generated metadata such as primary key IDs and audit timestamps).
3. **Decomposed Explainability**: No black-box monolithic scores. Every match score decomposes into an auditable hierarchy of explicit components (Required Skill Coverage, Preferred Skill Coverage) backed by sanitized evidence spans.
4. **Architectural PII & Evidence Isolation**: The scoring algorithm operates strictly on approved structured attributes (`skillId`, `isMandatory`). Textual evidence is strictly display metadata and has zero influence on the numerical score. Evidence is sanitized against a deterministic PII scrubber before persistence.
5. **Historical Legacy ML Preservation**: The deterministic engine **never reads, uses, or overwrites** historical legacy ML/SBERT scores. Historical records remain immutable.
6. **Resource Authorization Boundary**: Phase 4 defines and enforces resource-level tenant/department boundaries at the service layer, preparing for full authentication integration.
7. **Strict Batch Size Limits**: Candidate batch sizes are strictly bounded to 1–100 candidates (synchronous); requests with >100 candidates return HTTP 422 (`BATCH_SIZE_LIMIT_EXCEEDED`).
8. **No Decision Automation**: The engine acts solely as transparent decision assistance for recruiters, not as an autonomous hiring or rejection agent.

---

## 2. Mathematical Formalism & Notation

### 2.1 Universe of Discourse
Let $\mathcal{S}$ denote the finite universe of canonical skills defined in the FairHire AI skill taxonomy (as managed by Phase 3B):
$$\mathcal{S} = \{ s_1, s_2, \dots, s_N \}$$
where each skill $s \in \mathcal{S}$ has a unique canonical identifier $id(s) \in \mathbb{N}$ (represented in the database as `BIGINT` / `Long`), a canonical name $name(s)$, a category $cat(s)$, and an active status $active(s) \in \{true, false\}$. For Phase 4 matching, only active canonical skills are considered:
$$\mathcal{S}_{active} = \{ s \in \mathcal{S} \mid active(s) = true \}$$

### 2.2 Job Requirement Representation
A Job $J$ defines two disjoint finite subsets of $\mathcal{S}_{active}$:
- **Required Skills** ($R_J \subseteq \mathcal{S}_{active}$): Mandatory skills necessary for candidate baseline consideration.
- **Preferred Skills** ($P_J \subseteq \mathcal{S}_{active}$): Nice-to-have or bonus skills that enhance candidate qualification.

**Disjointness Invariant:**
$$R_J \cap P_J = \emptyset$$
If a recruiter or automated extractor marks a skill $s$ as both required and preferred, the matching engine enforces precedence: $s$ is assigned to $R_J$, and $P_J \leftarrow P_J \setminus \{s\}$.

### 2.3 Candidate Profile Representation
A Candidate (Resume) $C$ is associated with a set of extracted canonical skills $S_C \subseteq \mathcal{S}_{active}$.
Each skill $s \in S_C$ is equipped with metadata from Phase 3B extraction:
$$\forall s \in S_C, \quad \mathcal{M}_C(s) = \langle conf_C(s), snippet_C(s), matchedText_C(s), method_C(s) \rangle$$
where:
- $conf_C(s) \in [0.0000, 1.0000]$: Extraction confidence score (e.g., $1.0000$ for EXACT_CANONICAL_MATCH, $0.9500$ for EXACT_ALIAS_MATCH).
- $snippet_C(s)$: Verbatim textual context snippet from the resume validating skill extraction (`context_snippet`).
- $matchedText_C(s)$: Verbatim matched string from resume text (`matched_text`).
- $method_C(s) \in \{\text{EXACT\_CANONICAL\_MATCH}, \text{EXACT\_ALIAS\_MATCH}, \text{CASE\_INSENSITIVE\_CANONICAL\_MATCH}, \text{CASE\_INSENSITIVE\_ALIAS\_MATCH}, \text{NORMALIZED\_MATCH}\}$.

### 2.4 Set-Theoretic Partitioning
Evaluating candidate $C$ against job $J$ partitions the job requirements into four mutually exclusive sets:

1. **Matched Required Skills** ($M_{req}$):
   $$M_{req} = R_J \cap S_C$$
2. **Missing Required Skills** ($U_{req}$):
   $$U_{req} = R_J \setminus S_C$$
3. **Matched Preferred Skills** ($M_{pref}$):
   $$M_{pref} = P_J \cap S_C$$
4. **Missing Preferred Skills** ($U_{pref}$):
   $$U_{pref} = P_J \setminus S_C$$

**Partition Invariants:**
$$M_{req} \cup U_{req} = R_J, \quad M_{req} \cap U_{req} = \emptyset$$
$$M_{pref} \cup U_{pref} = P_J, \quad M_{pref} \cap U_{pref} = \emptyset$$

---

## 3. Scoring Specification & Standardized Precision

### 3.1 Arithmetic Rules & Database Types
All intermediate calculations must be performed using arbitrary-precision arithmetic (`java.math.BigDecimal` in Java). IEEE 754 floating-point types (`float`, `double`) are **strictly prohibited** in the scoring execution path to prevent platform-dependent round-off discrepancies.

Standardized types across database, entities, and API responses:
- **`required_skill_coverage`**: `NUMERIC(5, 4)` representing a normalized ratio from $0.0000$ to $1.0000$ (`scale = 4`, `RoundingMode.HALF_UP`).
- **`preferred_skill_coverage`**: `NUMERIC(5, 4)` representing a normalized ratio from $0.0000$ to $1.0000$ (`scale = 4`, `RoundingMode.HALF_UP`).
- **`overall_score`**: `NUMERIC(5, 2)` representing a percentage from $0.00$ to $100.00$ (`scale = 2`, `RoundingMode.HALF_UP`).

### 3.2 Coverage Formulations

#### 3.2.1 Required Skill Coverage ($Cov_{req}$)
For $|R_J| > 0$:
$$Cov_{req} = \frac{|M_{req}|}{|R_J|}$$

#### 3.2.2 Preferred Skill Coverage ($Cov_{pref}$)
For $|P_J| > 0$:
$$Cov_{pref} = \frac{|M_{pref}|}{|P_J|}$$

### 3.3 Complete Job Requirement Configurations & Scoring Formulas

The scoring engine evaluates jobs across four exhaustive structural categories:

| Configuration Category | Condition | Required Weight ($w_{req}$) | Preferred Weight ($w_{pref}$) | Composite Score Formula ($Score_{raw}$) | Final Overall Score ($Score_{final} \in [0.00, 100.00]$) | API Status |
|---|---|---|---|---|---|---|
| **Standard Configuration** | $|R_J| > 0 \land |P_J| > 0$ | $0.8000$ | $0.2000$ | $(0.8000 \times Cov_{req}) + (0.2000 \times Cov_{pref})$ | $\text{round}(Score_{raw} \times 100, 2)$ | HTTP 200 OK |
| **Required-Only Specification** | $|R_J| > 0 \land |P_J| = 0$ | $1.0000$ | $0.0000$ | $1.0000 \times Cov_{req}$ | $\text{round}(Score_{raw} \times 100, 2)$ | HTTP 200 OK |
| **Preferred-Only Specification** | $|R_J| = 0 \land |P_J| > 0$ | $0.0000$ | $1.0000$ | $1.0000 \times Cov_{pref}$ | $\text{round}(Score_{raw} \times 100, 2)$ | HTTP 200 OK |
| **Zero-Requirement Job** | $|R_J| = 0 \land |P_J| = 0$ | N/A | N/A | **Undefined / Unmatchable** | **None** | **HTTP 422 Unprocessable Entity** (`MATCHING_REQUIREMENTS_NOT_FOUND`) |

### 3.4 Experience & Non-Scored Metadata Boundary (Resolution of OQ-04)
- `minimum_years` from `job_skills` and `years_experience` from `resumes` **MUST NOT** contribute to $Score_{raw}$ or $Score_{final}$ in Phase 4.
- Experience metadata remains available strictly for informational display in the explanation payload.
- Experience scoring is formally deferred until a deterministic experience extraction and validation engine is specified and approved.

---

## 4. PII Isolation & Evidence Architecture

### 4.1 Scoring Engine Isolation Boundary
The deterministic scoring engine is completely decoupled from resume text, candidate identity, and textual evidence spans:

```
[Candidate Resume] ──> [Phase 3B Extraction] ──> [resume_skills]
                                                       │
                                  ┌────────────────────┴────────────────────┐
                                  ▼                                         ▼
                     [Structured IDs Only]                       [Context Snippets]
                    {skillId, isMandatory}                                  │
                                  │                                         ▼
                                  ▼                                 [PII Scrubber]
                    ┌───────────────────────────┐                           │
                    │   Deterministic Scoring   │                           ▼
                    │          Engine           │                   [Sanitized Snippet]
                    │  (Pure Math / Invariant)  │                           │
                    └─────────────┬─────────────┘                           │
                                  │                                         │
                                  ▼                                         ▼
                           overallScore,                       match_skill_details
                           componentCoverage                    (Explanation Metadata Only)
```

**Architectural Invariant:**
The scoring engine method signature accepts only structured sets of identifiers:
```java
public MatchScoreResult computeScore(Set<Long> candidateSkillIds, 
                                     Set<Long> requiredSkillIds, 
                                     Set<Long> preferredSkillIds);
```
Neither textual evidence, candidate names, nor email addresses are passed to or read by the calculation method.

### 4.2 Evidence Sanitization Architecture: Architecture B (Sanitized Before Persistence)
Evidence handling follows **Architecture B (Sanitized Before Persistence)**:
1. **Extraction Source**: The raw `context_snippet` is captured by Phase 3B (up to 300 characters around the matched skill token).
2. **Sanitization Execution**: Prior to inserting records into `match_skill_details`, the evidence snippet is processed by `DeterministicPiiScrubber`.
3. **Scrubber Rules**:
   - Replaces email patterns with `[REDACTED_EMAIL]`
   - Replaces phone patterns (E.164, US, International) with `[REDACTED_PHONE]`
   - Replaces physical addresses and postal codes with `[REDACTED_ADDRESS]`
   - Replaces personal URLs and social handles with `[REDACTED_URL]`
   - Truncates context to a maximum of 150 characters centered on the skill keyword
4. **Guarantees**:
   - Evidence stored in `match_skill_details` is free of personally identifiable information.
   - Database dumps, audit logs, and REST responses never leak unsanitized candidate PII through evidence fields.

---

## 5. Legacy SBERT Isolation, Non-Overwriting Invariant & Result Staleness

### 5.1 Legacy SBERT Isolation & Historical Non-Overwriting Invariant

**Explicit Architectural Invariant:**
> **"The deterministic matching engine never reads, uses, or overwrites historical legacy ML/SBERT scores."**

1. **Non-Overwriting Rule**: Historical records in `match_results` created with legacy ML/SBERT fields (`semantic_score`, `model_version = 'all-MiniLM-L6-v2'`) MUST NOT be overwritten, deleted, or mutated. They remain permanently preserved as historical audit records with their original `matching_method = 'SEMANTIC'`.
2. **Isolation Rule**: When calculating new matches, the deterministic engine never reads or incorporates `semantic_score`, legacy `model_version`, or legacy `keyword_score`.
3. **Zero Network Dependency**: Phase 4 services retain **ZERO** runtime or network dependency on `http://localhost:8000/embed` or any external embedding service.
4. **Distinct Identity**: Deterministic results are persisted with:
   - `algorithm_version = "deterministic-v1"`
   - `matching_method = "CANONICAL_SKILL_COVERAGE"`
   - `screening_mode = "NORMAL"`

### 5.2 Match Result Versioning & Staleness Lifecycle

To ensure recruiters never evaluate candidates against outdated requirements or stale resumes, Phase 4 introduces explicit staleness semantics:

#### 5.2.1 Staleness Flag
The `match_results` table maintains an explicit tracking column:
- `is_stale BOOLEAN NOT NULL DEFAULT FALSE`

#### 5.2.2 Staleness Invalidation Triggers
**Formal Staleness Invalidation Rule:**
> **"Any job mutation that can change the extracted/matched requirement set invalidates existing deterministic match results."**

Specifically, a match result record transitions from `is_stale = false` (Fresh) to `is_stale = true` (Stale) under two specific conditions:
1. **Job Requirement Mutation**: When `JobService.updateJob()` modifies a job's title, raw description, explicit skill requirements, or requirement necessity (`REQUIRED` vs `PREFERRED`), all existing `match_results` for that `job_id` are atomically marked `is_stale = true`.
2. **Candidate Skill Mutation**: When a candidate resume is re-parsed or re-extracted (updating `resume_skills`), all existing `match_results` for that `resume_id` are atomically marked `is_stale = true`.

#### 5.2.3 Recalculation Semantics
- When a recruiter triggers matching (`POST /api/v1/jobs/{jobId}/matches`), the engine recalculates the score using current active `job_skills` and `resume_skills`.
- Recalculation idempotently updates the existing deterministic record for `(job_id, resume_id, matching_method='CANONICAL_SKILL_COVERAGE')`:
  - `overall_score`, `required_skill_coverage`, `preferred_skill_coverage` are recalculated.
  - `required_skills_matched`, `required_skills_total`, `preferred_skills_matched`, `preferred_skills_total` are updated.
  - `is_stale` is reset to `false`.
  - `scored_at` is set to `Instant.now()`.
  - `match_skill_details` rows: MatchSkillDetail rows are immutable within a deterministic match-result evaluation. When a deterministic match is recalculated, the previous detail set is atomically replaced by a fresh detail set associated with the recalculated result.
- Recalculation **never** creates duplicate rows for the same `matching_method` and **never** touches legacy ML rows.

---

## 6. Resource Authorization vs. Authentication & Bounded Execution

### 6.1 Authentication vs. Resource Authorization Boundary
The current system configuration in `SecurityConfig.java` defines `/api/**` as `permitAll()`.

**Explicit Architectural Clarification:**
> **Phase 4 defines and implements the resource-authorization service boundary, but because authentication infrastructure currently permits all requests (`permitAll()`), end-to-end authenticated authorization enforcement cannot be claimed until authentication infrastructure exists.**

- **What Phase 4 Enforces Now (Service Boundary Validation)**:
  1. Department/tenant resource checking at the service layer: A request specifies a target `job_id` and optional `resume_ids`. The service verifies that all target resumes and jobs belong to the same department/organization boundary.
  2. Rejection of cross-department and cross-tenant matching: If an API caller requests matching a resume from Department B against a job in Department A, the service layer rejects the request with `HTTP 403 FORBIDDEN` (`UNAUTHORIZED_RESOURCE_ACCESS`).
  3. Pre-flight hook ready for SecurityContext principal binding: The service accepts an authenticated user context parameter, ready to bind to JWT/Session identity once authentication is configured.
- **What Depends on the Authentication Phase**:
  1. Cryptographically verifying the caller's identity via JWT tokens or session cookies.
  2. Populating `SecurityContextHolder` with verified recruiter organization and department claims.
  3. Declarative `@PreAuthorize` filter-chain evaluation.
  *We do NOT claim that `permitAll()` provides authenticated resource isolation.*

### 6.2 Bounded Batch Execution Semantics (1–100 Limit)

To guarantee predictability and prevent resource exhaustion without prematurely introducing distributed queues, Phase 4 strictly bounds batch execution:

- **1–100 Candidates (Synchronous Execution)**:
  - Processed synchronously in memory within the HTTP request lifecycle.
  - Chunked persistence transactions (e.g., 20 candidates per sub-transaction) prevent long-lived DB table locks.
  - Returns `HTTP 200 OK` with evaluation results.
- **>100 Candidates (Bounded Rejection)**:
  - If a recruiter requests matching for a candidate pool exceeding 100 resumes, matching execution is **aborted immediately**.
  - Returns `HTTP 422 UNPROCESSABLE ENTITY` with structured error code:
    ```json
    {
      "status": 422,
      "error": "BATCH_SIZE_LIMIT_EXCEEDED",
      "message": "Requested candidate batch size (105) exceeds maximum synchronous limit (100). Filter candidates by department, status, or explicit resume IDs."
    }
    ```
- **No Asynchronous Worker Infrastructure**: Phase 4 does NOT introduce background message queues or worker pools (e.g., RabbitMQ, Kafka). A future phase may add asynchronous matching for larger candidate pools.

---

## 7. Architecture Test Oracles Catalog (15 Formal Oracles)

The following 15 test oracles define the exact expected behavior of the Phase 4 engine. Unit and integration tests must validate against these exact specifications.

### Oracle 1: PII Isolation Oracle
- **Job**: Required = `{Java}`, Preferred = `{}`
- **Candidate A**: Skills = `{Java}`, Raw Evidence = `"John Doe, john@example.com, 555-1234, Senior Java Dev"`
- **Candidate B**: Skills = `{Java}`, Raw Evidence = `"Jane Smith, jane@corp.org, 555-9876, Java Architect"`
- **Assertion**:
  - $Score(Candidate\ A) == 100.00$
  - $Score(Candidate\ B) == 100.00$
  - Score Delta $\Delta = |Score(A) - Score(B)| \equiv 0.00$
  - Persisted context snippet in `match_skill_details` (`candidate_context_snippet`) contains `[REDACTED_EMAIL]` and `[REDACTED_PHONE]`.

### Oracle 2: Evidence Immunity Oracle
- **Job**: Required = `{Java, Spring Boot}`, Preferred = `{}`
- **Candidate 1**: Skills = `{Java}`, Evidence = `"Short Java snippet"`
- **Candidate 2**: Skills = `{Java}`, Evidence = `"Extremely long 500-word essay with buzzwords and repeated Java tokens"`
- **Assertion**:
  - $Score(Candidate\ 1) == 50.00$
  - $Score(Candidate\ 2) == 50.00$
  - Evidence text length and content have zero score impact.

### Oracle 3: Zero-Requirement Job Oracle
- **Job**: Required = `{}`, Preferred = `{}` ($|R_J| = 0, |P_J| = 0$)
- **Candidate**: Skills = `{Java, Python, Docker}`
- **Assertion**:
  - Matching execution aborted.
  - Return: `HTTP 422 Unprocessable Entity`.
  - Error Body: `{ "error": "MATCHING_REQUIREMENTS_NOT_FOUND", "message": "Job has zero recognized canonical skill requirements" }`.
  - Never produces a false $100\%$ score.

### Oracle 4: Required-Only Scoring Oracle
- **Job**: Required = `{Java, Spring Boot, PostgreSQL}`, Preferred = `{}` ($|R_J| = 3, |P_J| = 0$)
- **Candidate**: Skills = `{Java, Spring Boot}`
- **Assertion**:
  - $w_{req} = 1.0000, \quad w_{pref} = 0.0000$
  - $Cov_{req} = \frac{2}{3} \approx 0.6667$
  - $Score_{raw} = 1.0000 \times 0.6667 = 0.6667$
  - $Score_{final} == 66.67$

### Oracle 5: Preferred-Only Scoring Oracle
- **Job**: Required = `{}`, Preferred = `{Docker, Kubernetes}` ($|R_J| = 0, |P_J| = 2$)
- **Candidate**: Skills = `{Docker}`
- **Assertion**:
  - $w_{req} = 0.0000, \quad w_{pref} = 1.0000$
  - $Cov_{pref} = \frac{1}{2} = 0.5000$
  - $Score_{raw} = 1.0000 \times 0.5000 = 0.5000$
  - $Score_{final} == 50.00$

### Oracle 6: Perfect Match Oracle
- **Job**: Required = `{Java, SQL}`, Preferred = `{Docker}` ($|R_J| = 2, |P_J| = 1$)
- **Candidate**: Skills = `{Java, SQL, Docker, Python, AWS}`
- **Assertion**:
  - $Cov_{req} = \frac{2}{2} = 1.0000$
  - $Cov_{pref} = \frac{1}{1} = 1.0000$
  - $Score_{raw} = (0.8000 \times 1.0000) + (0.2000 \times 1.0000) = 1.0000$
  - $Score_{final} == 100.00$
  - Extraneous skills (`Python`, `AWS`) do not inflate score beyond $100.00$.

### Oracle 7: Partial Match Oracle (Standard Balanced Case)
- **Job**: Required = `{Java, Spring Boot, PostgreSQL, Docker}` ($|R_J| = 4$), Preferred = `{AWS, Kubernetes}` ($|P_J| = 2$)
- **Candidate**: Skills = `{Java, Spring Boot, AWS}`
- **Assertion**:
  - $Cov_{req} = \frac{2}{4} = 0.5000$
  - $Cov_{pref} = \frac{1}{2} = 0.5000$
  - $Score_{raw} = (0.8000 \times 0.5000) + (0.2000 \times 0.5000) = 0.4000 + 0.1000 = 0.5000$
  - $Score_{final} == 50.00$

### Oracle 8: Zero Candidate Skills Oracle
- **Job**: Required = `{Java, SQL}`, Preferred = `{Docker}` ($|R_J| = 2, |P_J| = 1$)
- **Candidate**: Skills = `{}` ($|S_C| = 0$)
- **Assertion**:
  - $Cov_{req} = 0.0000$
  - $Cov_{pref} = 0.0000$
  - $Score_{raw} = 0.0000$
  - $Score_{final} == 0.00$

### Oracle 9: Legacy SBERT Preservation Oracle (Do Not Overwrite History)
- **Scenario**: Database contains an existing historical record with `job_id = 7`, `resume_id = 3`, `screening_mode = 'NORMAL'`, `matching_method = 'SEMANTIC'`, `semantic_score = 0.9450`, and `model_version = 'all-MiniLM-L6-v2'`.
- **Trigger**: Recruiter triggers Phase 4 deterministic matching for Job 7 and Resume 3.
- **Assertion**:
  - Historical row with `matching_method = 'SEMANTIC'` remains **completely untouched and unmodified** (`semantic_score` remains `0.9450`, `model_version` remains `'all-MiniLM-L6-v2'`).
  - Deterministic matcher creates/updates its distinct row keyed by `matching_method = 'CANONICAL_SKILL_COVERAGE'`, `algorithm_version = 'deterministic-v1'`, `screening_mode = 'NORMAL'`.
  - The historical `semantic_score` is never read, incorporated, or overwritten.

### Oracle 10: Deterministic Repeated Evaluation Oracle
- **Scenario**: Evaluate identical Job and Candidate inputs 1,000 consecutive times across multiple threads.
- **Assertion**:
  - **Deterministic equality comparison** verifies that:
    - `overall_score`
    - `required_skill_coverage`
    - `preferred_skill_coverage`
    - `required_skills_matched`
    - `required_skills_total`
    - `preferred_skills_matched`
    - `preferred_skills_total`
    - ordered `match_skill_details` (sorted by `job_skill_id ASC`, `skill_id ASC`)
    - explanation tree content
    are identical across all runs.
  - Runtime metadata (generated primary key IDs, `scored_at` timestamps) are excluded from equality checks.

### Oracle 11: Duplicate Skills Deduplication Oracle
- **Job**: Recruiter enters Required = `{Java, Java, Java}`
- **Candidate**: Extracted Skills = `{Java, Java}`
- **Assertion**:
  - Pre-evaluation set normalization reduces Job to $|R_J| = 1$ and Candidate to $|S_C| = 1$.
  - $Cov_{req} = \frac{1}{1} = 1.0000$.
  - $Score_{final} == 100.00$.

### Oracle 12: Required / Preferred Overlap Resolution Oracle
- **Job**: Recruiter specifies `Java` as both Required and Preferred.
- **Candidate**: Skills = `{Java}`
- **Assertion**:
  - Precedence rule assigns `Java` to Required ($R_J$) and purges from Preferred ($P_J$).
  - Overlap is eliminated ($R_J \cap P_J = \emptyset$).
  - If no other preferred skills remain, job evaluates as Required-Only ($w_{req} = 1.00, w_{pref} = 0.00$).

### Oracle 13: Stale-Result Invalidation Oracle
- **Scenario**: Job 7 initially requires `{Java}`. Candidate matches with score $100.00$ (`is_stale = false`).
- **Trigger 1**: Recruiter updates Job 7 to require `{Java, Kotlin}` via `JobService.updateJob()`.
- **Assertion 1**: Job 7's match results are atomically marked with `is_stale = true`.
- **Trigger 2**: Recruiter re-triggers matching for Job 7.
- **Assertion 2**: Match is re-evaluated; score updates to $50.00$, and `is_stale` is reset to `false`.

### Oracle 14: Resource Authorization Boundary Oracle
- **Scenario**: Request caller attempts to match Candidate Resume 12 (Department "Engineering") against Job 9 (Department "Finance").
- **Trigger**: Caller invokes `POST /api/v1/jobs/9/matches` with `{ "resume_ids": [12] }`.
- **Assertion**:
  - Execution is blocked at service boundary.
  - Return: `HTTP 403 FORBIDDEN` (`UNAUTHORIZED_RESOURCE_ACCESS`).
  - No matching calculation or candidate resume access occurs.

### Oracle 15: Batch Size Limit (>100) Oracle
- **Scenario**: Recruiter triggers matching for Job 7 against a candidate pool containing 105 active resumes.
- **Assertion**:
  - Execution is aborted immediately prior to evaluation.
  - Return: `HTTP 422 UNPROCESSABLE ENTITY`.
  - Error Body: `{ "error": "BATCH_SIZE_LIMIT_EXCEEDED", "message": "Requested candidate batch size (105) exceeds maximum synchronous limit (100)..." }`.
  - Zero database writes occur.

---

## 8. Database DDL Specification (Flyway V5)

To be placed in `backend/src/main/resources/db/migration/V5__phase_4_matching_engine.sql`:

```sql
-- =========================================================================
-- FairHire AI - Migration V5: Phase 4 Deterministic Matching Engine
-- =========================================================================

-- 1. Structured metadata extensions on jobs (OQ-05)
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS location VARCHAR(150);
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS employment_type VARCHAR(50);
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS remote_policy VARCHAR(30);

-- 2. Structured extraction metadata extensions on job_skills
ALTER TABLE job_skills ADD COLUMN IF NOT EXISTS extraction_method VARCHAR(50);
ALTER TABLE job_skills ADD COLUMN IF NOT EXISTS context_snippet VARCHAR(300);
ALTER TABLE job_skills ADD COLUMN IF NOT EXISTS matched_text VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_job_skills_skill_id ON job_skills(skill_id);

-- 3. Phase 4 deterministic scoring, staleness, and versioning columns on match_results
ALTER TABLE match_results ADD COLUMN IF NOT EXISTS required_skill_coverage NUMERIC(5, 4);
ALTER TABLE match_results ADD COLUMN IF NOT EXISTS preferred_skill_coverage NUMERIC(5, 4);
ALTER TABLE match_results ADD COLUMN IF NOT EXISTS overall_score NUMERIC(5, 2);
ALTER TABLE match_results ADD COLUMN IF NOT EXISTS required_skills_total INTEGER;
ALTER TABLE match_results ADD COLUMN IF NOT EXISTS required_skills_matched INTEGER;
ALTER TABLE match_results ADD COLUMN IF NOT EXISTS preferred_skills_total INTEGER;
ALTER TABLE match_results ADD COLUMN IF NOT EXISTS preferred_skills_matched INTEGER;
ALTER TABLE match_results ADD COLUMN IF NOT EXISTS is_stale BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE match_results ADD COLUMN IF NOT EXISTS algorithm_version VARCHAR(50) DEFAULT 'deterministic-v1';
ALTER TABLE match_results ADD COLUMN IF NOT EXISTS scored_at TIMESTAMPTZ;

-- 4. Granular per-skill match explanation table (BIGINT IDs matching PostgreSQL schema)
CREATE TABLE IF NOT EXISTS match_skill_details (
    id                        BIGSERIAL PRIMARY KEY,
    match_result_id           BIGINT NOT NULL REFERENCES match_results(id) ON DELETE CASCADE,
    job_skill_id              BIGINT NOT NULL REFERENCES job_skills(id) ON DELETE CASCADE,
    skill_id                  BIGINT NOT NULL REFERENCES skills(id) ON DELETE RESTRICT,
    necessity                 VARCHAR(10) NOT NULL CHECK (necessity IN ('REQUIRED', 'PREFERRED')),
    is_matched                BOOLEAN NOT NULL,
    candidate_confidence      NUMERIC(4, 3) CHECK (
                                  candidate_confidence IS NULL OR
                                  (candidate_confidence >= 0.0 AND candidate_confidence <= 1.0)),
    candidate_matched_text    VARCHAR(100),
    candidate_context_snippet VARCHAR(300),
    CONSTRAINT uq_match_skill_details_result_jobskill UNIQUE (match_result_id, job_skill_id)
);

-- 5. Expand matching_method CHECK constraint to support CANONICAL_SKILL_COVERAGE
ALTER TABLE match_results DROP CONSTRAINT IF EXISTS match_results_matching_method_check;
ALTER TABLE match_results ADD CONSTRAINT match_results_matching_method_check 
    CHECK (matching_method IN ('KEYWORD', 'SEMANTIC', 'HYBRID', 'CANONICAL_SKILL_COVERAGE'));

-- 6. Indexes for query performance, explainability, and staleness filtering
CREATE INDEX IF NOT EXISTS idx_match_results_job_score 
    ON match_results(job_id, overall_score DESC);

CREATE INDEX IF NOT EXISTS idx_match_results_resume_job 
    ON match_results(resume_id, job_id);

CREATE INDEX IF NOT EXISTS idx_match_results_job_stale 
    ON match_results(job_id, is_stale);

CREATE INDEX IF NOT EXISTS idx_match_skill_details_match_result 
    ON match_skill_details(match_result_id);

CREATE INDEX IF NOT EXISTS idx_match_skill_details_skill 
    ON match_skill_details(skill_id);

CREATE INDEX IF NOT EXISTS idx_match_skill_details_necessity 
    ON match_skill_details(match_result_id, necessity);

CREATE INDEX IF NOT EXISTS idx_job_skills_job_mandatory 
    ON job_skills(job_id, is_mandatory);
```

**Existing Unique Constraint Compatibility Analysis:**
- The actual existing unique constraint in PostgreSQL schema (from `V1__initial_schema.sql`) is:
  `CONSTRAINT uq_match_results_job_resume_mode_method UNIQUE (job_id, resume_id, screening_mode, matching_method)`
- Because `matching_method` is explicitly part of the composite unique constraint:
  - Historical ML rows retain `matching_method = 'SEMANTIC'` (or `'KEYWORD'`, `'HYBRID'`).
  - Phase 4 deterministic rows are persisted with `matching_method = 'CANONICAL_SKILL_COVERAGE'`.
  - Both records naturally coexist for the same `(job_id, resume_id, screening_mode)` without collision or schema conflict.
- The existing unique constraint is fully compatible as-is; only the `CHECK` constraint on `matching_method` requires the additive update above.

---

## 9. Formal Resolution of Open Questions (OQ-01 through OQ-08)

| ID | Open Question | Final Decision | Rationale |
|---|---|---|---|
| **OQ-01** | Behavior when a job description yields zero recognized canonical skills? | **A zero-requirement job is unmatchable.** Return `HTTP 422 UNPROCESSABLE ENTITY` with error code `MATCHING_REQUIREMENTS_NOT_FOUND`. | Prevents zero-requirement jobs from generating misleading $100\%$ or default match scores. Enforces that only jobs with at least one canonical skill requirement can be matched. |
| **OQ-02** | Default weight allocation between Required and Preferred skills? | **Baseline 0.80 Required / 0.20 Preferred approved.** If only required skills exist, $w_{req} = 1.00$. If only preferred skills exist, $w_{pref} = 1.00$. | Standardizes evaluation while dynamically normalizing for required-only or preferred-only specifications without dividing by zero. |
| **OQ-03** | Continuous confidence weighting vs. Binary presence? | **Binary Presence (Option A) approved.** Each matched skill contributes $1.0$ to coverage. Extraction confidence is preserved strictly as informational evidence metadata. | Maximizes transparency and explainability; prevents opaque score discounting while keeping extraction confidence visible for human audit. |
| **OQ-04** | Role of `minimum_years` experience in numerical score? | **Excluded from Phase 4 scoring.** `minimum_years` is informational display metadata only. Experience scoring is deferred to a future phase. | Heuristic experience parsing is not yet verified or deterministic. Excludes potential proxy age bias from the scoring engine. |
| **OQ-05** | Add `location`, `employment_type`, and `remote_policy` to `jobs` in Flyway V5? | **Approved.** Additive columns included in Flyway migration `V5__phase_4_matching_engine.sql`. | Enables structured job metadata without altering frozen historical migrations (`V1`–`V4`). |
| **OQ-06** | Batch matching vs. Single candidate matching lifecycle? | **Explicit recruiter-triggered matching.** Job creation does NOT trigger batch evaluation. Matching is triggered via explicit `POST /api/v1/jobs/{jobId}/matches`. | Decouples job authoring from scoring computation; gives recruiters control to review extracted requirements before running evaluation. |
| **OQ-07** | Fate of legacy `/api/matching` controller? | **Deprecated.** Replaced by `/api/v1/jobs/{id}/matches`. All legacy SBERT integration decoupled. | Establishes standard `/v1/` REST conventions and eliminates active dependencies on `http://localhost:8000/embed`. |
| **OQ-08** | Handling of historical SBERT `match_results` rows? | **Retain in database as historical audit records without overwriting.** Deterministic engine writes with `matching_method = 'CANONICAL_SKILL_COVERAGE'` and `algorithm_version = 'deterministic-v1'`. | Upholds data integrity and audit trail; guarantees deterministic engine never reads, uses, or overwrites legacy ML scores. |

---

## 10. Implementation Governance & Gate Status

```
============================================================
PHASE 4 STATUS:
ARCHITECTURE DESIGN COMPLETE — AWAITING APPROVAL
============================================================
```

- **Architecture Document**: [docs/architecture/phase-4-architecture.md](file:///c:/Users/harhm/Downloads/FairHire_AI_Application/fairhire_ai/docs/architecture/phase-4-architecture.md) (Updated)
- **Matching Specification**: [docs/architecture/phase-4-matching-spec.md](file:///c:/Users/harhm/Downloads/FairHire_AI_Application/fairhire_ai/docs/architecture/phase-4-matching-spec.md) (Updated)
- **ID Type Consistency**: All entity identifiers standardized on `BIGINT` / `Long` (`id(s) \in \mathbb{N}`).
- **Precision Standardized**: `overall_score` is `NUMERIC(5, 2)` (0.00–100.00); coverage metrics are `NUMERIC(5, 4)` (0.0000–1.0000).
- **Algorithm Version**: Standardized globally to `"deterministic-v1"`.
- **Implementation Code**: **LOCKED / NOT IMPLEMENTED**
