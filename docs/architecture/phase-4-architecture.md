# FairHire AI — Phase 4 Architecture Design
## Job–Candidate Matching & Fairness Engine

**Status:** `ARCHITECTURE DESIGN COMPLETE — AWAITING APPROVAL`
**Date:** 2026-10-05
**Author:** Phase 4 Architecture Audit
**Depends On:** Phase 3A (resume ingestion), Phase 3B (deterministic skill extraction — APPROVED)

---

## Table of Contents

1. Goals
2. Non-Goals
3. Repository Audit Findings
4. Current System Integration
5. What Already Exists (Phase 3B Contract)
6. What Must Change or Be Added
7. Domain Model
8. ER Diagram
9. Job Model
10. Candidate Model
11. Job Skill Extraction Design
12. Matching Model
13. Scoring Proposal
14. Explainability Model
15. Fairness Boundaries
16. API Proposal
17. Database Proposal
18. Migration Plan
19. Determinism Requirements
20. Performance Considerations
21. Security Considerations
22. Testing Strategy
23. Risks
24. Open Questions
25. Recommended Implementation Sequence

---

## 1. Goals

Phase 4 introduces the first job-candidate matching capability in FairHire AI.

- Provide a **transparent, deterministic, explainable** matching foundation.
- Consume **Phase 3B canonical skill outputs** as the primary matching signal.
- Produce **structured match results** with decomposed score components and evidence.
- Support **skill coverage** (required vs. preferred), **missing skill identification**, and **per-skill evidence linking**.
- Treat fairness as a **first-class architectural constraint**, not a post-hoc filter.
- Make every match result **auditable** and answerable: *"Why did candidate A receive this result?"*

---

## 2. Non-Goals

The following are explicitly **forbidden** in Phase 4:

| Forbidden | Reason |
|---|---|
| Demographic inference (name, gender, age, disability, religion, political, ethnicity, socioeconomic) | Discriminatory, illegal in many jurisdictions |
| Protected-attribute prediction of any kind | Prohibited |
| Facial analysis | Prohibited |
| Opaque candidate ranking | Conflicts with explainability requirement |
| Automated hiring or rejection decisions | Phase 4 is decision **assistance**, not decision-making |
| "Culture fit" scoring | Undefined, proxy for bias |
| Semantic embeddings (SBERT, Sentence-BERT, etc.) | Not explicitly approved |
| LLM-based candidate scoring | Not explicitly approved |
| TF-IDF similarity | Not explicitly approved |
| Jaccard similarity | Not explicitly approved |
| Black-box ML ranking | Not explicitly approved |
| External candidate data enrichment | Not explicitly approved |
| Bias mitigation algorithms without defined spec | Not explicitly approved |

---

## 3. Repository Audit Findings

### 3.1 Backend — What Exists

#### Domain Model

| Entity | Table | Status | Phase 4 Relevance |
|---|---|---|---|
| `Job` | `jobs` | Complete | REUSE |
| `JobSkill` | `job_skills` | Complete | REUSE + EXTEND (add evidence fields) |
| `JobRequirement` | `job_requirements` | Complete | REUSE |
| `Skill` | `skills` | Complete (31 active) | REUSE — immutable canonical taxonomy |
| `ResumeSkill` | `resume_skills` | Complete | REUSE — confidence, evidence, method |
| `Resume` | `resumes` | Complete | REUSE — raw text, years experience, status |
| `Candidate` | `candidates` | Complete | REUSE — name, email (display only) |
| `MatchResult` | `match_results` | Problematic — see §3.2 | EXTEND |
| `User` | `users` | Complete | REUSE |
| `AuditLog` | `audit_logs` | Complete | EXTEND for Phase 4 events |

#### Services

| Service | Status | Phase 4 Relevance |
|---|---|---|
| `DeterministicSkillExtractor` | Optimized (BitSet, Phase 3B) | REUSE for job description extraction |
| `SkillExtractionService` | Complete | REUSE (wrap for job extraction) |
| `SkillPersistenceService` | Complete | PARTIAL REUSE |
| `JobService.createJob()` | Naive `String.contains()` matching | REPLACE with DeterministicSkillExtractor |
| `MatchingService.runJobMatching()` | Calls AIServiceClient for SBERT | REPLACE with deterministic skill coverage |

### 3.2 Critical Problems Found

**P-01: MatchingService has hard SBERT dependency**

```java
// MatchingService.java line 65-68 — throws if AI service unavailable
Map<String, Object> semResult = aiServiceClient.computeSemanticScore(
    job.getDescription(), resume.getRawText());
```

Problem: Zero matching capability without SBERT. Phase 4 engine must not depend on the AI service.

**P-02: MatchResult schema encodes SBERT assumptions**

`match_results` has `semantic_score`, `keyword_score`, `model_version = 'all-MiniLM-L6-v2'`. These conflict with Phase 4 deterministic model. Decision: extend with new nullable columns via V5 migration; leave legacy columns zero-filled.

**P-03: JobService.createJob() uses naive String.contains() for job skill association**

```java
for (Skill sk : allSkills) {
    if (combinedText.contains(sk.getName().toLowerCase())) {
        job.getJobSkills().add(new JobSkill(job, sk)); // ignores boundaries/aliases/disambiguation
    }
}
```

Must be replaced with `DeterministicSkillExtractor`.

**P-04: JobSkill.is_mandatory boolean is opaque**
`is_mandatory = true` encodes REQUIRED, `false` encodes PREFERRED. Schema is acceptable but must be explicitly documented and validated.

**P-05: API path inconsistency**
- Resume endpoints: `/api/v1/resumes`
- Job endpoints: `/api/jobs` (missing `/v1/`)
- Matching endpoints: `/api/matching` (missing `/v1/`)

Phase 4 must use `/api/v1/` prefix consistently.

**P-06: MatchingController route conflict**
`GET /api/matching/{id}` and `GET /api/matching/{jobId}/results` share a path template ambiguity. Phase 4 redesign must fix this.

**P-07: match_results.rank_in_pool requires cross-candidate pre-computation**
Rank depends on all other candidates being scored first. Phase 4 must document rank computation strategy.

### 3.3 Frontend — What Exists

| Component | Relevance |
|---|---|
| `RecruiterDashboard.jsx` | Primary workspace — needs job management and match views |
| `CandidatePortal.jsx` | May need match status view |
| `ResultsDashboard.jsx` | Phase 4 needs dedicated match results view |
| `AuditHistory.jsx` | May extend for match audit trail |

Frontend is a React SPA with view-switching via `useState`. Phase 4 UI adds views within existing pattern.

### 3.4 Database — Migration State

| Migration | Description | Checksum | Status |
|---|---|---|---|
| V1 | Initial schema | (recorded) | FROZEN |
| V2 | Resume ingestion lifecycle | (recorded) | FROZEN |
| V3 | Skill taxonomy + extraction metadata | `11453495` | FROZEN — MUST NOT CHANGE |
| V4 | Taxonomy cleanup, constraint fix | (recorded) | FROZEN |
| **V5** | **Phase 4 matching schema** | **TBD** | **PROPOSED** |

---

## 4. Current System Integration

```
Phase 3A/3B (Approved)
  Resume Upload -> Parser -> DeterministicSkillExtractor
      -> resume_skills (canonical, with confidence)

              | Phase 4 consumes resume_skills
              v

Phase 4 (Proposed)
  Job created -> DeterministicSkillExtractor (reused)
      -> job_skills (required/preferred, with evidence)
  MatchingEngine: resume_skills intersect job_skills
      -> match_skill_details (per-skill evidence)
      -> match_results (extended: score components)
```

---

## 5. What Already Exists (Phase 3B Contract)

Phase 4 **must consume** the following Phase 3B outputs without modification:

| Output | Source | Guaranteed Properties |
|---|---|---|
| Canonical skill ID | `resume_skills.skill_id` | Stable FK to `skills.id` |
| Canonical skill name | `skills.name` | Unique, controlled vocabulary |
| Extraction confidence | `resume_skills.extraction_confidence` | 0.880-1.000 tier-based |
| Evidence snippet | `resume_skills.context_snippet` | Raw resume text window |
| Matched text | `resume_skills.matched_text` | Exact text matched |
| Extraction method | `resume_skills.extraction_method` | `SkillExtractionMethod` enum |
| Deduplication | UQ `(resume_id, skill_id)` | One row per skill per resume |

**Preservation guarantees:**
- Do not re-extract skills already in `resume_skills`.
- Do not create a second skill taxonomy.
- Do not modify `SkillExtractionMethod` confidence tiers.
- Do not add canonical skills without a migration.

---

## 6. What Must Change or Be Added

| Component | Change Type | Reason |
|---|---|---|
| `JobService.createJob()` skill extraction | REPLACE | Naive contains() -> DeterministicSkillExtractor |
| `MatchingService.runJobMatching()` | REPLACE | SBERT dependency -> deterministic skill coverage |
| `MatchingController` | REPLACE | Path prefix, route conflict, SBERT weights |
| `match_results` table | EXTEND (V5) | Add decomposed score columns |
| New table: `match_skill_details` | CREATE (V5) | Per-skill match evidence |
| New service: `SkillCoverageMatchingService` | CREATE | Core Phase 4 engine |
| New DTOs: `MatchRequestDto`, `MatchResponseDto`, `MatchSkillDetailDto` | CREATE | Typed API contracts |
| `JobController` path prefix | FIX | `/api/jobs` -> `/api/v1/jobs` |
| `JobSkill` entity | EXTEND | Add `extraction_method`, `context_snippet`, `matched_text` |

---

## 7. Domain Model

### 7.1 Reused Without Change

| Entity | Why Unchanged |
|---|---|
| `Skill` | Canonical taxonomy — immutable |
| `Resume` | Full Phase 3B lifecycle |
| `ResumeSkill` | Complete with confidence, evidence, method |
| `Candidate` | Contact metadata only |
| `User` | Auth and role |
| `AuditLog` | Extended with new event types |

### 7.2 Reused With Extension

| Entity | Extension |
|---|---|
| `Job` | No schema change; lifecycle changes in validation |
| `JobSkill` | Add `extraction_method VARCHAR(50)`, `context_snippet VARCHAR(300)`, `matched_text VARCHAR(100)` |
| `JobRequirement` | No change |
| `MatchResult` | Add `required_skill_coverage`, `preferred_skill_coverage`, `overall_score`, `algorithm_version`, `scored_at` |

### 7.3 New in Phase 4

| Entity | Purpose |
|---|---|
| `MatchSkillDetail` | One row per (match, job_skill) pair. Records whether each job skill was matched, candidate evidence, confidence. Enables per-skill explainability. |

### 7.4 Entities NOT Created (with justification)

| Entity Name | Reason NOT Created |
|---|---|
| `CandidateProfile` | `Candidate` + `Resume` + `ResumeSkill` already sufficient. Duplication with no benefit. |
| `MatchExplanation` | Explanation is derived from `MatchSkillDetail` rows + `MatchResult` score components. Separate entity would duplicate data. |
| `SkillRequirement` | Redundant with `JobSkill.isMandatory` + `RequirementNecessity`. |

### 7.5 Lifecycle Ownership

| Entity | Created By | Updated By | Deleted By |
|---|---|---|---|
| `Job` | Recruiter via POST /api/v1/jobs | Recruiter via PUT /api/v1/jobs/{id} | Recruiter via DELETE |
| `JobSkill` | Auto-extracted on job create/update | Re-extracted on job update | Cascades on job delete |
| `MatchResult` | POST /api/v1/jobs/{id}/matches | Re-run overwrites via upsert | Cascades on job delete |
| `MatchSkillDetail` | Created with MatchResult | MatchSkillDetail rows are immutable within a deterministic match-result evaluation. When a deterministic match is recalculated, the previous detail set is atomically replaced by a fresh detail set associated with the recalculated result. | Cascades on match delete |

---

## 8. ER Diagram

```
PHASE 3B (existing, immutable)
  skills (id PK, name UNIQUE, category, synonyms[], is_active)
      ^
      |
  resume_skills (resume_id FK, skill_id FK, extraction_confidence,
                 context_snippet, matched_text, extraction_method)
      |
  resumes (id PK, candidate_id FK, raw_text, parsed_years_experience,
           parsing_status)
      |
  candidates (id PK, user_id FK, full_name, email)

PHASE 4 (proposed V5 migration)
  job_skills (EXTENDED)
    job_id FK -> jobs.id
    skill_id FK -> skills.id
    is_mandatory BOOLEAN   (true=REQUIRED, false=PREFERRED)
    minimum_years INT
    weight NUMERIC(4,3)
    [NEW] extraction_method VARCHAR(50)
    [NEW] context_snippet VARCHAR(300)
    [NEW] matched_text VARCHAR(100)
    UNIQUE (job_id, skill_id)

  match_results (EXTENDED)
    id PK BIGSERIAL
    job_id FK -> jobs.id
    resume_id FK -> resumes.id
    candidate_id FK -> candidates.id
    [EXISTING] keyword_score, semantic_score, ...  (legacy, nullable)
    [NEW] required_skill_coverage NUMERIC(5,4)
    [NEW] preferred_skill_coverage NUMERIC(5,4)
    [NEW] overall_score NUMERIC(5,2)
    [NEW] required_skills_total INT
    [NEW] required_skills_matched INT
    [NEW] preferred_skills_total INT
    [NEW] preferred_skills_matched INT
    [NEW] is_stale BOOLEAN DEFAULT FALSE
    [NEW] algorithm_version VARCHAR(50)
    [NEW] scored_at TIMESTAMPTZ

  match_skill_details  (NEW TABLE)
    id PK BIGSERIAL
    match_result_id FK -> match_results.id  ON DELETE CASCADE
    job_skill_id FK -> job_skills.id  ON DELETE CASCADE
    skill_id FK -> skills.id  ON DELETE RESTRICT
    necessity VARCHAR(10)  ('REQUIRED' | 'PREFERRED')
    is_matched BOOLEAN
    candidate_confidence NUMERIC(4,3)  (null if not matched)
    candidate_matched_text VARCHAR(100)
    candidate_context_snippet VARCHAR(300)
    UNIQUE (match_result_id, job_skill_id)
```

---

## 9. Job Model

### 9.1 Existing Fields (Reused)

| Field | Type | Notes |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `title` | VARCHAR(200) | |
| `department` | VARCHAR(100) | Optional |
| `raw_description` | TEXT | Full JD text |
| `rewritten_description` | TEXT | Bias-neutral rewrite (optional) |
| `status` | ENUM(DRAFT, ACTIVE, CLOSED, ARCHIVED) | Lifecycle state |
| `creator_id` | FK -> users | |

### 9.2 Candidate New Fields (Open Question OQ-05)

| Field | Type | Notes |
|---|---|---|
| `location` | VARCHAR(150) | Nullable |
| `employment_type` | VARCHAR(50) | Nullable |
| `remote_policy` | VARCHAR(30) | Nullable |

These are **proposed only**. If not approved, Phase 4 matching is skills-only.

### 9.3 Required vs. Preferred Skills

`job_skills.is_mandatory`:
- `true` = REQUIRED skill
- `false` = PREFERRED skill

This boolean encoding is acceptable. Phase 4 enforces it via validation. No schema type change required.

---

## 10. Candidate Model

### 10.1 Features Allowed in Matching

| Feature | Source | Usage |
|---|---|---|
| Canonical skills | `resume_skills.skill_id` | PRIMARY matching signal |
| Extraction confidence | `resume_skills.extraction_confidence` | Confidence-weighted coverage |
| Years experience | `resumes.parsed_years_experience` | Experience gate (informational) |
| Skill evidence | `resume_skills.context_snippet` | Explainability only, not scoring |

### 10.2 Features Forbidden in Matching

| Feature | Reason |
|---|---|
| `candidates.full_name` | Could encode gender, ethnicity |
| `candidates.email` | Could encode demographics |
| `resumes.file_name` | Identity proxy |
| `resumes.raw_text` as semantic signal | Opaque, could embed demographics |
| `resumes.parsed_education` | Free text; prestige proxy risk |
| Any `blind_screening_results` field | Covers identity |

---

## 11. Job Skill Extraction Design

### 11.1 Reuse DeterministicSkillExtractor

Phase 4 will invoke `DeterministicSkillExtractor.extractSkills(job.getRawDescription(), activeSkills)` directly. This provides:
- Canonical name matching with word boundary enforcement
- Alias/synonym matching (79 aliases)
- Case-insensitive matching
- Disambiguation (C, Go, Node.js rules)
- Overlap resolution via BitSet
- Confidence tiers (1.000 to 0.880)
- Context snippet capture

No new extraction logic is written.

### 11.2 Required vs. Preferred Signal

Phase 4 API allows recruiters to explicitly mark job skills as REQUIRED or PREFERRED. Automatic inference from free text is NOT in scope.

Auto-extracted skills default to `is_mandatory = true` (REQUIRED).

### 11.3 Unrecognized Terms & Zero-Requirement Jobs (Resolution of OQ-01)

Terms in the job description not matching any canonical skill are ignored. 

**Crucial Rule:** A job with zero recognized canonical skill requirements ($|R_J| = 0 \land |P_J| = 0$) **MUST NOT be matchable**.
Attempting to evaluate or trigger matching on a zero-requirement job returns `HTTP 422 UNPROCESSABLE ENTITY` with a structured error payload:
```json
{
  "error": "MATCHING_REQUIREMENTS_NOT_FOUND",
  "message": "Job has zero recognized canonical skill requirements. Matching is unavailable."
}
```
A zero-requirement job will **never** produce a misleading 100% or default score.

### 11.4 Extraction Trigger

Skills are extracted at job creation time and re-extracted on job update. `job_skills` rows are replaced atomically on update.

### 11.5 Duplicate Handling & Requirement Overlap

- Enforced by `UNIQUE(job_id, skill_id)` constraint. Identical to resume skill deduplication.
- If a skill is marked as both REQUIRED and PREFERRED, the engine enforces precedence: the skill is assigned to REQUIRED, and purged from PREFERRED.

---

## 12. Matching Model

### 12.1 What Constitutes a Match

A skill is **matched** for a candidate on a job if and only if:
- The canonical `skill_id` appears in `resume_skills` for the candidate's resume
- That resume has `parsing_status = READY`
- `Skill.is_active = true`

No fuzzy matching. No partial matching. No synonym expansion beyond what was already done at extraction time (extraction already normalized aliases to canonical IDs).

### 12.2 Matching Algorithm — Canonical Skill Coverage

```
R = set of canonical skill_ids in resume_skills for resume X
J_req = set of canonical skill_ids in job_skills where is_mandatory = true
J_pref = set of canonical skill_ids in job_skills where is_mandatory = false

MATCHED_REQ = R intersect J_req
MISSING_REQ = J_req minus R
MATCHED_PREF = R intersect J_pref
MISSING_PREF = J_pref minus R

For |J_req| > 0:
  required_coverage = |MATCHED_REQ| / |J_req|

For |J_pref| > 0:
  preferred_coverage = |MATCHED_PREF| / |J_pref|
```

### 12.3 Exact Scoring Formulas Across Configurations

The engine evaluates jobs across four exhaustive structural categories:

1. **Standard Configuration ($|J_{req}| > 0 \land |J_{pref}| > 0$):**
   $$Score_{raw} = (0.8000 \times required\_coverage) + (0.2000 \times preferred\_coverage)$$
   $$Score_{final} = \text{round}(Score_{raw} \times 100, 2)$$

2. **Required-Only Specification ($|J_{req}| > 0 \land |J_{pref}| = 0$):**
   $$Score_{raw} = 1.0000 \times required\_coverage$$
   $$Score_{final} = \text{round}(Score_{raw} \times 100, 2)$$

3. **Preferred-Only Specification ($|J_{req}| = 0 \land |J_{pref}| > 0$):**
   $$Score_{raw} = 1.0000 \times preferred\_coverage$$
   $$Score_{final} = \text{round}(Score_{raw} \times 100, 2)$$

4. **Zero-Requirement Job ($|J_{req}| = 0 \land |J_{pref}| = 0$):**
   Matching unavailable. Returns `HTTP 422 UNPROCESSABLE ENTITY` (`MATCHING_REQUIREMENTS_NOT_FOUND`).

### 12.4 Confidence Integration (Resolution of OQ-03)

**Decision: Binary Presence (Option A).**
Each matched skill contributes a binary $1.0$ toward the coverage ratio.
Extraction confidence scores (e.g., $1.000$ EXACT, $0.950$ EXACT_ALIAS) are preserved strictly as **informational evidence metadata** in `match_skill_details` for recruiter auditing. Confidence values **do not** scale or discount the candidate's mathematical score.

### 12.5 Experience Handling (Resolution of OQ-04)

`minimum_years` from `job_skills` and `years_experience` from `resumes` **MUST NOT** contribute to the numerical match score in Phase 4. 
Experience attributes remain available solely as informational display metadata. Experience scoring is deferred until a separately specified deterministic experience extraction system exists.

### 12.6 Edge Cases Catalog

| Case | Condition | Behavior |
|---|---|---|
| Zero-requirement job | $|J_{req}| = 0 \land |J_{pref}| = 0$ | Abort matching; return HTTP 422 `MATCHING_REQUIREMENTS_NOT_FOUND` |
| Required-only job | $|J_{req}| > 0 \land |J_{pref}| = 0$ | $w_{req} = 1.00, w_{pref} = 0.00$; score = $1.00 \times required\_coverage$ |
| Preferred-only job | $|J_{req}| = 0 \land |J_{pref}| > 0$ | $w_{req} = 0.00, w_{pref} = 1.00$; score = $1.00 \times preferred\_coverage$ |
| Candidate has 0 skills | $|R| = 0$ | $required\_coverage = 0.0000, preferred\_coverage = 0.0000, overall\_score = 0.00$ |
| Perfect match | $J_{req} \cup J_{pref} \subseteq R$ | $overall\_score = 100.00$ (extraneous skills do not inflate beyond 100) |
| Resume parsing_status != READY | Resume not ready | Excluded from candidate matching pool |
| Duplicate match request | (job_id, resume_id) | Idempotent upsert — updates existing match record |
| Overlapping skill requirement | Skill in both req & pref | Precedence assigns skill to required; purged from preferred |

### 12.7 Legacy SBERT Isolation & Non-Overwriting Invariant

The existing `match_results` table contains historical semantic fields (`semantic_score`, `model_version = 'all-MiniLM-L6-v2'`).

**Hard Architectural Invariants:**
1. **Non-Overwriting Invariant:**
   > **"The deterministic matching engine never reads, uses, or overwrites historical legacy ML/SBERT scores."**
2. Historical records created during legacy ML prototyping (`matching_method = 'SEMANTIC'`) remain permanently preserved as immutable historical records.
3. Phase 4 deterministic services retain **ZERO** runtime or network dependency on `http://localhost:8000/embed` or any external embedding service.
4. New matches explicitly identify their algorithm via:
   - `algorithm_version = "deterministic-v1"`
   - `matching_method = "CANONICAL_SKILL_COVERAGE"`
   - `screening_mode = "NORMAL"`

### 12.8 Match Result Versioning & Staleness Lifecycle

To ensure recruiters never evaluate candidates against outdated requirements or stale resumes, Phase 4 introduces explicit staleness semantics:

#### 12.8.1 Staleness Flag
`match_results` maintains an explicit tracking column: `is_stale BOOLEAN NOT NULL DEFAULT FALSE`.

#### 12.8.2 Staleness Invalidation Triggers
**Formal Staleness Invalidation Rule:**
> **"Any job mutation that can change the extracted/matched requirement set invalidates existing deterministic match results."**

A match result record transitions from `is_stale = false` (Fresh) to `is_stale = true` (Stale) under two specific conditions:
1. **Job Requirement Mutation**: When `JobService.updateJob()` modifies a job's title, raw description, explicit skill requirements, or requirement necessity (`REQUIRED` vs `PREFERRED`), all existing `match_results` for that `job_id` are atomically updated to `is_stale = true`.
2. **Candidate Skill Mutation**: When a candidate resume is re-parsed or re-extracted (updating `resume_skills`), all existing `match_results` for that `resume_id` are atomically updated to `is_stale = true`.

#### 12.8.3 Recalculation Semantics
- When matching is triggered via `POST /api/v1/jobs/{jobId}/matches`, the engine recalculates the score using current active `job_skills` and `resume_skills`.
- Recalculation idempotently updates the existing deterministic record for `(job_id, resume_id, matching_method='CANONICAL_SKILL_COVERAGE')`:
  - `overall_score`, `required_skill_coverage`, `preferred_skill_coverage` are recomputed.
  - `is_stale` is reset to `false`.
  - `scored_at` is set to `Instant.now()`.
  - `match_skill_details` rows: MatchSkillDetail rows are immutable within a deterministic match-result evaluation. When a deterministic match is recalculated, the previous detail set is atomically replaced by a fresh detail set associated with the recalculated result.
- Recalculation **never** overwrites or mutates historical legacy ML rows (`matching_method = 'SEMANTIC'`).

### 12.9 Determinism & Arithmetic Invariant

Given identical `resume_skills`, `job_skills`, and taxonomy state, the matching engine produces identical output components:
- `overall_score`
- `required_skill_coverage`
- `preferred_skill_coverage`
- `required_skills_matched` / `required_skills_total`
- `preferred_skills_matched` / `preferred_skills_total`
- ordered `match_skill_details`
- explanation tree content

Generated database IDs, `scored_at` / `evaluated_at` timestamps, and runtime transaction metadata are excluded from deterministic equality assertions.
All arithmetic uses `java.math.BigDecimal` with `scale = 4`, `RoundingMode.HALF_UP`. IEEE 754 floating point is strictly prohibited.

---

## 13. Scoring Proposal

### 13.1 Score Structure

```
overall_score: 50.00%
  required_skill_coverage: 0.5000 (weight: 0.8000)
    matched: [Java, Spring Boot] (2/4 = 0.5000)
    missing: [PostgreSQL, Docker]

  preferred_skill_coverage: 0.5000 (weight: 0.2000)
    matched: [AWS] (1/2 = 0.5000)
    missing: [Kubernetes]

overall_score = round(((0.8000 * 0.5000) + (0.2000 * 0.5000)) * 100, 2) = 50.00%
```

### 13.2 Standardized Score Precision & Database Types

- **`overall_score`**: `NUMERIC(5,2)` representing $0.00$ to $100.00$ (`scale = 2`, `RoundingMode.HALF_UP`, e.g., `50.00`).
- **`required_skill_coverage`**: `NUMERIC(5,4)` representing $0.0000$ to $1.0000$ (`scale = 4`, `RoundingMode.HALF_UP`, e.g., `0.5000`).
- **`preferred_skill_coverage`**: `NUMERIC(5,4)` representing $0.0000$ to $1.0000$ (`scale = 4`, `RoundingMode.HALF_UP`, e.g., `0.5000`).
- **API Response**: Returns `overall_score` as a percentage (e.g. `50.00`) alongside raw ratio components (`0.5000`).

### 13.3 No Opaque Single Score

The API response MUST always include:
- `overall_score`
- `required_skill_coverage`
- `preferred_skill_coverage`
- `required_skills_matched` (count)
- `required_skills_total` (count)
- `preferred_skills_matched` (count)
- `preferred_skills_total` (count)
- `algorithm_version`
- `match_skill_details[]` (per-skill rows)

---

## 14. Explainability Model

### 14.1 Per-Match Explanation Structure

```json
{
  "match_id": 42,
  "job_id": 7,
  "resume_id": 3,
  "algorithm_version": "deterministic-v1",
  "scored_at": "2026-10-05T12:00:00Z",
  "overall_score": 50.00,
  "required_skill_coverage": 0.5000,
  "preferred_skill_coverage": 0.5000,
  "required_skills_matched": 2,
  "required_skills_total": 4,
  "preferred_skills_matched": 1,
  "preferred_skills_total": 2,
  "required_skills": [
    {
      "skill_id": 1,
      "skill_name": "Java",
      "necessity": "REQUIRED",
      "is_matched": true,
      "candidate_confidence": 1.000,
      "candidate_matched_text": "Java",
      "candidate_context_snippet": "5 years of Java development experience"
    },
    {
      "skill_id": 24,
      "skill_name": "PostgreSQL",
      "necessity": "REQUIRED",
      "is_matched": false,
      "candidate_confidence": null,
      "candidate_matched_text": null,
      "candidate_context_snippet": null
    }
  ],
  "preferred_skills": [ ... ],
  "warnings": []
}
```

### 14.2 Explanation Consistency Guarantee

The explanation MUST be derived from the same data that produced the score. The scoring engine and explanation builder share a single execution path.

Invariants that must always hold:
- `required_skill_coverage == required_skills_matched / required_skills_total`
- `overall_score == round(((REQUIRED_WEIGHT * required_coverage) + (PREFERRED_WEIGHT * preferred_coverage)) * 100, 2)`
- Every `is_matched = true` skill has non-null `candidate_confidence` and `candidate_matched_text`
- Every `is_matched = false` skill has null `candidate_confidence`

### 14.3 PII & Evidence Isolation Architecture (Architecture B: Sanitized Before Persistence)

A critical architectural constraint in Phase 4 is that **evidence is display-only metadata and MUST NOT influence the match score**.

1. **Scoring Isolation Boundary**:
   The core scoring calculation method accepts **only** sets of canonical skill IDs (`Set<Long> candidateSkillIds`, `Set<Long> requiredSkillIds`, `Set<Long> preferredSkillIds`). Textual context snippets, candidate names, and emails are physically excluded from the calculation parameter list.
2. **Chosen Sanitization Architecture: Architecture B (Sanitized Before Persistence)**:
   - When building `MatchSkillDetail` records, candidate context snippets are routed through a `DeterministicPiiScrubber`.
   - The scrubber scrubs:
     - Emails (`[REDACTED_EMAIL]`)
     - Phone numbers (`[REDACTED_PHONE]`)
     - Street addresses and postal codes (`[REDACTED_ADDRESS]`)
     - Personal URLs and social links (`[REDACTED_URL]`)
   - The sanitized snippet is then persisted to `match_skill_details.candidate_context_snippet`.
   - **Benefit**: Protects the database at rest (DB backups, logs, and API payloads never contain unsanitized candidate PII), while keeping score calculation 100% immune to textual injection.

---

## 15. Fairness Boundaries

### 15.1 Features Allowed Into Scoring

| Feature | Reason | Allowed in Scoring |
|---|---|---|
| Canonical skill IDs | Objective, job-relevant qualifications | **YES** |
| `is_mandatory` (REQUIRED vs PREFERRED) | Structural job requirement specification | **YES** |
| Extraction confidence | Quality indicator; preserved as metadata only | **NO (Display only)** |
| Years experience (numeric) | Informational metadata only; deferred to future engine | **NO (Display only)** |
| Evidence context snippets | Explanation metadata only; sanitized via Architecture B | **NO (Display only)** |

### 15.2 Features Forbidden From Scoring

| Feature | Category | Prohibition |
|---|---|---|
| `candidates.full_name` | Identity | STRICTLY FORBIDDEN |
| `candidates.email` | Identity | STRICTLY FORBIDDEN |
| `candidates.phone` | Identity | STRICTLY FORBIDDEN |
| `resumes.file_name` | Identity proxy | STRICTLY FORBIDDEN |
| `resumes.raw_text` as semantic signal | Opaque proxy | FORBIDDEN without explicit approval |
| `resumes.parsed_education` | Prestige proxy | FORBIDDEN in Phase 4 |
| Any `blind_screening_results` field | Covers identity | FORBIDDEN as score input |
| Demographic inference of any kind | Protected attributes | FORBIDDEN |

### 15.3 Proxy Feature Risk Assessment

| Feature | Risk | Mitigation |
|---|---|---|
| Canonical skills | LOW | Restricted to approved taxonomy |
| Confidence values | VERY LOW | Excluded from mathematical score formula |
| Years experience | MEDIUM — age proxy | Excluded from Phase 4 scoring |
| `parsed_education` | HIGH — prestige bias | Excluded from scoring |

### 15.4 Audit Logging Requirements

Every match computation logs to `audit_logs`:

| Event | Fields |
|---|---|
| `MATCH_COMPUTED` | `entity_type=MATCH_RESULT`, `entity_id=match_result.id`, `details={job_id, resume_id, algorithm_version, overall_score}` |
| `JOB_SKILL_EXTRACTED` | `entity_type=JOB`, `entity_id=job.id`, `details={skill_count, extraction_method}` |

### 15.5 Model/Version Tracking & SBERT Deprecation

- `algorithm_version VARCHAR(50)` in `match_results` records which algorithm produced each result.
- Phase 4 initial version: `"deterministic-v1"`.
- `matching_method VARCHAR(50)` is set to `"CANONICAL_SKILL_COVERAGE"`.
- Zero runtime or network dependency on legacy SBERT embedding service (`http://localhost:8000/embed`).
- Any change to weights, algorithm logic, or coverage definition increments `algorithm_version`.

### 15.6 Score Reproducibility for Audit

Given `match_result.id`, any auditor can:
1. Read `match_skill_details` to inspect matched vs missing skills.
2. Read `job_skills` to view exact requirements.
3. Read `resume_skills` to view extracted skills.
4. Read `algorithm_version` to verify the mathematical formula applied.
5. Recompute and verify `overall_score` identically using arbitrary-precision `BigDecimal`.

---

## 16. API Proposal

All Phase 4 endpoints use the `/api/v1/` prefix.

### 16.1 Job Management

#### POST /api/v1/jobs

Request:
```json
{
  "title": "Senior Java Developer",
  "department": "Engineering",
  "description": "We need a Java and Spring Boot developer...",
  "required_skills": ["Java", "Spring Boot", "PostgreSQL"],
  "preferred_skills": ["AWS", "Docker"],
  "requirements": [
    { "type": "EXPERIENCE_YEARS", "value": "3", "necessity": "REQUIRED" },
    { "type": "EDUCATION_LEVEL", "value": "Bachelor's", "necessity": "PREFERRED" }
  ]
}
```

Notes:
- `required_skills` and `preferred_skills` are optional explicit overrides.
- If absent, skills are auto-extracted from `title + description` via `DeterministicSkillExtractor` (auto-extracted skills default to REQUIRED).
- Explicit skill lists take precedence over auto-extraction.

Response (201 Created):
```json
{
  "id": 7,
  "title": "Senior Java Developer",
  "department": "Engineering",
  "description": "...",
  "status": "ACTIVE",
  "required_skills": [{ "skill_id": 1, "name": "Java", "matched_text": "Java", "extraction_method": "EXACT_CANONICAL_MATCH" }],
  "preferred_skills": [{ "skill_id": 21, "name": "AWS", "matched_text": "AWS", "extraction_method": "EXACT_CANONICAL_MATCH" }],
  "requirements": [...],
  "created_at": "2026-10-05T12:00:00Z"
}
```

Validation errors (400):
- `title` blank or > 200 chars
- `description` blank
- Unknown skill names in explicit skill lists
- Same skill in both required_skills and preferred_skills

#### GET /api/v1/jobs

Query params: `?page=0&size=20&status=ACTIVE`
Response (200): Paginated list of job summaries.

#### GET /api/v1/jobs/{id}

Response (200): Full job DTO.
Response (404): Job not found.

#### PUT /api/v1/jobs/{id}

Request: Same shape as POST, all fields optional.
Response (200): Updated job DTO.
Note: Re-extracts skills if description changes. Existing match results become stale.

#### DELETE /api/v1/jobs/{id}

Response (200): `{ "message": "Job {id} deleted." }`
Response (404): Not found.
Note: Cascades to job_skills, job_requirements, match_results, match_skill_details.

### 16.2 Job Skills

#### GET /api/v1/jobs/{id}/skills

Response (200):
```json
{
  "job_id": 7,
  "required": [
    {
      "skill_id": 1,
      "name": "Java",
      "category": "PROGRAMMING_LANGUAGE",
      "minimum_years": 3,
      "matched_text": "Java",
      "context_snippet": "...",
      "extraction_method": "EXACT_CANONICAL_MATCH"
    }
  ],
  "preferred": [
    {
      "skill_id": 21,
      "name": "AWS",
      "category": "CLOUD_DEVOPS",
      "minimum_years": 0,
      "matched_text": "AWS",
      "context_snippet": "...",
      "extraction_method": "EXACT_CANONICAL_MATCH"
    }
  ]
}
```

### 16.3 Matching Endpoints & Recruiter Lifecycle (Resolution of OQ-05 / OQ-06)

#### Explicit Recruiter-Triggered Matching Lifecycle
Job creation and match evaluation are strictly decoupled. Creating a job **DOES NOT** trigger automated matching across the candidate pool.

```
POST /api/v1/jobs
        ↓
Job created & saved (HTTP 201)
        ↓
Recruiter reviews requirements in dashboard
        ↓
Recruiter explicitly requests matching
        ↓
POST /api/v1/jobs/{jobId}/matches
```

#### POST /api/v1/jobs/{id}/matches

- **Resource-Level Authorization Boundary**:
  - `ROLE_RECRUITER` / `ROLE_ADMIN` check alone is insufficient.
  - Recruiters may **only**:
    - Match resumes they are authorized to access (within their assigned department / tenant scope).
    - View match results for jobs they are authorized to access.
    - Inspect candidate evidence only within their authorized scope.
  - **Cross-tenant / cross-organization resume access is strictly prohibited.**
  - Enforced at the service boundary prior to query execution. Violations return `HTTP 403 FORBIDDEN` (`UNAUTHORIZED_RESOURCE_ACCESS`).
- **Batch Size Semantics & Bounded Execution (1–100 Limit)**:
  - **1–100 candidate resumes**: Executed synchronously in memory within the HTTP request lifecycle. Chunked persistence transactions allowed.
  - **>100 candidate resumes**: Request is rejected immediately with `HTTP 422 UNPROCESSABLE ENTITY` and error code `BATCH_SIZE_LIMIT_EXCEEDED`.
  - **No Asynchronous Worker Infrastructure**: Phase 4 does NOT introduce background message queues or async workers. A future phase may add asynchronous matching for larger candidate pools.
- **Candidate Selection**: Evaluates all resumes with `parsing_status = 'READY'` in the authorized scope. Optionally accepts `{ "resume_ids": [101, 102] }` in request body for targeted evaluation.
- **Idempotency & Duplicate Handling**: Match evaluation is idempotent. Upsert semantics keyed on `(job_id, resume_id, screening_mode, matching_method)` update existing rows and regenerate `match_skill_details`.
- **Transaction Boundaries**: Evaluations are processed in isolated transactional units per candidate. A database failure on candidate $N$ rolls back candidate $N$ without aborting or corrupting candidates $1 \dots N-1$.

Request (all candidates): `{}`  
Request (filtered candidate list): `{ "resume_ids": [3, 4, 7] }`

Success Response (200 OK):
```json
{
  "job_id": 7,
  "algorithm_version": "deterministic-v1",
  "matching_method": "CANONICAL_SKILL_COVERAGE",
  "total_evaluated": 5,
  "results": [
    {
      "match_id": 42,
      "resume_id": 3,
      "candidate_id": 12,
      "overall_score": 50.00,
      "required_skill_coverage": 0.5000,
      "preferred_skill_coverage": 0.5000,
      "required_skills_matched": 2,
      "required_skills_total": 4,
      "preferred_skills_matched": 1,
      "preferred_skills_total": 2,
      "is_stale": false,
      "scored_at": "2026-10-06T12:00:00Z"
    }
  ]
}
```

Error Response (422 Unprocessable Entity — Zero Requirement Job):
```json
{
  "status": 422,
  "error": "MATCHING_REQUIREMENTS_NOT_FOUND",
  "message": "Job 7 has zero recognized canonical skill requirements. Matching is unavailable."
}
```

Error Response (422 Unprocessable Entity — Batch Size Limit Exceeded):
```json
{
  "status": 422,
  "error": "BATCH_SIZE_LIMIT_EXCEEDED",
  "message": "Requested candidate batch size (105) exceeds maximum synchronous limit (100). Filter candidates by department, status, or explicit resume IDs."
}
```

Error Response (403 Forbidden): Unauthorized resource access or cross-tenant access attempt.
Error Response (404 Not Found): Job does not exist.

#### GET /api/v1/jobs/{id}/matches

Query params: `?page=0&size=20&sort=overall_score,desc`  
Response (200 OK): Paginated list of match summaries.  
Response (404 Not Found): Job not found.

#### GET /api/v1/jobs/{id}/matches/{resumeId}

Full decomposed match detail for a specific candidate resume, including sanitized `match_skill_details`.

Response (200): Full match DTO including required_skills[], preferred_skills[], warnings[].
Response (404): Job not found, resume not found, or no match exists.

---

## 17. Database Proposal

### 17.1 V5 Migration: job_skills Extension

```sql
ALTER TABLE job_skills
    ADD COLUMN IF NOT EXISTS extraction_method VARCHAR(50),
    ADD COLUMN IF NOT EXISTS context_snippet   VARCHAR(300),
    ADD COLUMN IF NOT EXISTS matched_text      VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_job_skills_skill_id ON job_skills(skill_id);
```

### 17.2 V5 Migration: match_results Extension

```sql
ALTER TABLE match_results
    ADD COLUMN IF NOT EXISTS required_skill_coverage  NUMERIC(5,4),
    ADD COLUMN IF NOT EXISTS preferred_skill_coverage NUMERIC(5,4),
    ADD COLUMN IF NOT EXISTS overall_score            NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS required_skills_total    INTEGER,
    ADD COLUMN IF NOT EXISTS required_skills_matched  INTEGER,
    ADD COLUMN IF NOT EXISTS preferred_skills_total   INTEGER,
    ADD COLUMN IF NOT EXISTS preferred_skills_matched INTEGER,
    ADD COLUMN IF NOT EXISTS is_stale                 BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS algorithm_version        VARCHAR(50) DEFAULT 'deterministic-v1',
    ADD COLUMN IF NOT EXISTS scored_at                TIMESTAMPTZ;

-- Expand CHECK constraint to allow CANONICAL_SKILL_COVERAGE
ALTER TABLE match_results DROP CONSTRAINT IF EXISTS match_results_matching_method_check;
ALTER TABLE match_results ADD CONSTRAINT match_results_matching_method_check
    CHECK (matching_method IN ('KEYWORD', 'SEMANTIC', 'HYBRID', 'CANONICAL_SKILL_COVERAGE'));
```

**Existing Unique Constraint Compatibility Analysis:**
- The actual existing unique constraint in PostgreSQL schema (from `V1__initial_schema.sql`) is:
  `CONSTRAINT uq_match_results_job_resume_mode_method UNIQUE (job_id, resume_id, screening_mode, matching_method)`
- Because `matching_method` is explicitly part of the composite unique constraint:
  - Historical ML rows retain `matching_method = 'SEMANTIC'` (or `'KEYWORD'`, `'HYBRID'`).
  - Phase 4 deterministic rows are persisted with `matching_method = 'CANONICAL_SKILL_COVERAGE'`.
  - Both records naturally coexist for the same `(job_id, resume_id, screening_mode)` without collision or schema conflict.
- The existing unique constraint is fully compatible as-is; only the `CHECK` constraint on `matching_method` requires the additive update above.

### 17.3 V5 Migration: match_skill_details (New Table)

```sql
CREATE TABLE match_skill_details (
    id                        BIGSERIAL PRIMARY KEY,
    match_result_id           BIGINT NOT NULL REFERENCES match_results(id) ON DELETE CASCADE,
    job_skill_id              BIGINT NOT NULL REFERENCES job_skills(id) ON DELETE CASCADE,
    skill_id                  BIGINT NOT NULL REFERENCES skills(id) ON DELETE RESTRICT,
    necessity                 VARCHAR(10) NOT NULL CHECK (necessity IN ('REQUIRED', 'PREFERRED')),
    is_matched                BOOLEAN NOT NULL,
    candidate_confidence      NUMERIC(4,3) CHECK (
                                  candidate_confidence IS NULL OR
                                  (candidate_confidence >= 0.0 AND candidate_confidence <= 1.0)),
    candidate_matched_text    VARCHAR(100),
    candidate_context_snippet VARCHAR(300),
    CONSTRAINT uq_match_skill_details_result_jobskill UNIQUE (match_result_id, job_skill_id)
);

CREATE INDEX idx_match_skill_details_match_result
    ON match_skill_details(match_result_id);
CREATE INDEX idx_match_skill_details_skill
    ON match_skill_details(skill_id);
CREATE INDEX idx_match_skill_details_necessity
    ON match_skill_details(match_result_id, necessity);
```

### 17.4 V5 Migration: Optional Job Fields (OQ-05)

```sql
-- Only include if approved
ALTER TABLE jobs
    ADD COLUMN IF NOT EXISTS location        VARCHAR(150),
    ADD COLUMN IF NOT EXISTS employment_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS remote_policy   VARCHAR(30);
```

### 17.5 Index Summary

| Index | Table | Columns | Purpose |
|---|---|---|---|
| `idx_job_skills_skill_id` | `job_skills` | `skill_id` | Skill lookup across jobs |
| `idx_match_skill_details_match_result` | `match_skill_details` | `match_result_id` | Detail fetch per match |
| `idx_match_skill_details_skill` | `match_skill_details` | `skill_id` | Cross-match analytics |
| `idx_match_skill_details_necessity` | `match_skill_details` | `match_result_id, necessity` | Required/preferred filter |

---

## 18. Migration Plan

| Step | Action | Risk |
|---|---|---|
| 1 | Create `V5__phase4_matching_schema.sql` | LOW — additive only |
| 2 | Extend `job_skills` with nullable evidence columns | LOW — backward compatible |
| 3 | Extend `match_results` with nullable Phase 4 score columns | LOW — backward compatible |
| 4 | Create `match_skill_details` table | LOW — new table |
| 5 | Add optional job fields (if approved) | LOW — nullable additions |
| 6 | Verify V3 checksum `11453495` unchanged | VERIFICATION STEP |

**V3 checksum must remain `11453495`.** V5 does not touch V3.

---

## 19. Determinism Requirements

1. Matching engine uses `java.math.BigDecimal` with explicit `RoundingMode.HALF_UP` and `scale = 4`.
2. Set intersection uses `java.util.Set<Long>` (skill IDs), not skill names.
3. `match_skill_details` rows are created in deterministic order (sorted by `job_skill.skill_id ASC`).
4. `algorithm_version` is a compile-time constant `"deterministic-v1"`.
5. No `Math.random()`, `UUID.randomUUID()`, or time-dependent logic in score computation.
6. Tests explicitly assert: same inputs => same outputs, across 100 independent invocations.

---

## 20. Performance Considerations

### 20.1 One Candidate vs. One Job

- Complexity: O(J + R) where J = job skills count (max 31), R = resume skills count (max 31).
- Expected latency: < 5 ms per candidate.

### 20.2 One Job vs. Many Candidates

- `POST /api/v1/jobs/{id}/matches` fetches all READY resumes in one query with JOIN FETCH resume_skills.
- At 1,000 candidates with 20 skills each: ~20,000 set operations — trivial.
- No caching required at Phase 4 scale.

### 20.3 Query Patterns

| Query | Strategy |
|---|---|
| All resumes for matching | `findByParsingStatus(READY)` with JOIN FETCH resumeSkills |
| Job skills for a job | `findByJobId()` |
| Match result for (job, resume) | Unique constraint lookup |
| All match results for a job, sorted | `findByJobIdOrderByOverallScoreDesc()` |
| Match skill details | `findByMatchResultId()` |

### 20.4 N+1 Prevention

- `Resume` must be fetched with `JOIN FETCH resumeSkills` to avoid N+1 queries.
- `JobSkill` must be fetched with the `Job` via JOIN FETCH at match time.

---

## 21. Security Considerations

1. **Authentication vs. Resource Authorization**:
   - **Service Boundary Defined**: Phase 4 defines and implements the resource-authorization service boundary (`ResourceAuthorizationService` / departmental and tenant scope checks on jobs and resumes).
   - **Current `permitAll()` Configuration**: The current security configuration has `SecurityConfig` with `.requestMatchers("/api/**", ...).permitAll()`. While Phase 4 defines and implements the programmatic resource-authorization service boundary, because authentication infrastructure is still `permitAll`, end-to-end authenticated authorization enforcement cannot be claimed until authentication exists.
   - **Enforcement Scope**: Phase 4 enforces structural caller context validation (e.g. valid tenant/department headers or context if supplied, throwing `UNAUTHORIZED_RESOURCE_ACCESS` on cross-tenant attempts). Full authenticated resource isolation depends on the upcoming authentication phase.
2. **Candidate Data Exposure**: Match scoring functions strictly isolate candidate identity; `candidates.full_name`, `email`, phone, and demographic attributes are never loaded or evaluated during scoring. In the current open-auth (`permitAll()`) model, endpoint display fields are visible without login — an acknowledged architectural boundary that depends on the authentication phase.
3. **Input Validation**: Job `description` input must be validated for max length (50,000 chars) to prevent pathological inputs to `DeterministicSkillExtractor`.
4. **Audit Trail**: All match computations are logged via `audit_logs` with actor and timestamp.

---

## 22. Testing Strategy

### 22.1 Unit Tests

| Test Class | Scope |
|---|---|
| `SkillCoverageMatchingEngineTest` | Core algorithm — required coverage, preferred coverage, overall score, all edge cases |
| `MatchExplainabilityTest` | Explanation fields arithmetically consistent with scores |
| `JobSkillExtractionTest` | DeterministicSkillExtractor reuse for job descriptions |
| `MatchDeterminismTest` | 100 identical invocations => identical outputs |

Required unit test edge cases:
- Job with 0 required skills, 0 preferred skills => abort with `MATCHING_REQUIREMENTS_NOT_FOUND` (422)
- Candidate with 0 skills => required_coverage = 0.0000, preferred_coverage = 0.0000, overall_score = 0.00
- Required-only job (no preferred skills) => score = 1.00 * required_coverage
- Preferred-only job (no required skills) => score = 1.00 * preferred_coverage
- Standard job => score = (0.80 * required_coverage) + (0.20 * preferred_coverage)

### 22.2 Integration Tests

| Test | Scope |
|---|---|
| `JobCreationIntegrationTest` | Job created => skills extracted => job_skills persisted with evidence |
| `MatchingIntegrationTest` | Job + resumes in DB => match computed => match_results + match_skill_details persisted |
| `MatchResultPersistenceTest` | Upsert — re-running match overwrites existing result idempotently |
| `ParsingStatusGateTest` | Only READY resumes included in matching |

### 22.3 API Tests

| Endpoint | Test |
|---|---|
| POST /api/v1/jobs | 201 created; 400 on blank title; 400 on blank description |
| GET /api/v1/jobs/{id} | 200 with skills; 404 on unknown id |
| GET /api/v1/jobs/{id}/skills | 200 with required/preferred split |
| POST /api/v1/jobs/{id}/matches | 200 with results; 404 on unknown job; 422 if job has 0 recognized skills |
| GET /api/v1/jobs/{id}/matches/{resumeId} | 200 full detail; 404 on unknown |
| POST with non-READY resume | Resume excluded from results |

### 22.4 Fairness/Safety Tests

| Test | Assertion |
|---|---|
| `ForbiddenAttributeTest` | `candidates.full_name`, `email` never appear in score computation path |
| `PiiInjectionImmunityTest` | Injecting PII or changing evidence text produces $\Delta = 0.0000$ score change |
| `ExplanationConsistencyTest` | `overall_score == round(((0.80 * required_coverage) + (0.20 * preferred_coverage)) * 100, 2)` always |
| `ScoreReproducibilityTest` | 1,000 match runs on same data => identical deterministic fields: overall_score, coverages, counts, ordered match_skill_details, explanation content (excluding generated IDs, timestamps, runtime metadata) |
| `NoDemographicInferenceTest` | No field from `blind_screening_results` read during matching |
| `LegacySbertIsolationTest` | Historical `semantic_score` in DB has zero impact on Phase 4 deterministic output |

### 22.5 Regression Tests

All Phase 3A/3B tests must continue passing:

```
Tests run: 75, Failures: 0, Errors: 0, Skipped: 0
```

No Phase 3B test may be modified or removed. Phase 4 adds new test classes only.

### 22.6 Architecture Test Oracles Catalog (15 Formal Oracles)

1. **PII Isolation Oracle**: Candidate A and Candidate B have identical skills `{Java}` but Candidate A's raw evidence contains `"John Doe, john@test.com, 555-0100"`. Both produce identical score $100.00$. Context snippet persisted in `match_skill_details` (`candidate_context_snippet`) is redacted to `[REDACTED_EMAIL]`, `[REDACTED_PHONE]`.
2. **Evidence Immunity Oracle**: Altering evidence snippet text or length produces $\Delta = 0.0000$ change in match score.
3. **Zero-Requirement Job Oracle**: Job with $|R_J| = 0 \land |P_J| = 0$ returns `HTTP 422 UNPROCESSABLE ENTITY` (`MATCHING_REQUIREMENTS_NOT_FOUND`). Never produces a false $100\%$ score.
4. **Required-Only Scoring Oracle**: Job with 3 required, 0 preferred skills. Candidate has 2 required skills. $Score = \frac{2}{3} \approx 66.67\%$.
5. **Preferred-Only Scoring Oracle**: Job with 0 required, 2 preferred skills. Candidate has 1 preferred skill. $Score = \frac{1}{2} = 50.00\%$.
6. **Perfect Match Oracle**: Candidate satisfies all required and preferred skills. $Score = 100.00\%$. Extraneous skills do not exceed $100\%$.
7. **Partial Match Oracle**: Job has 4 required, 2 preferred. Candidate matches 2 required, 1 preferred. $Score = (0.80 \times 0.50) + (0.20 \times 0.50) = 50.00\%$.
8. **Zero Candidate Skills Oracle**: Candidate with 0 extracted skills evaluates to $0.00\%$.
9. **Legacy SBERT Preservation Oracle (Do Not Overwrite History)**: An existing historical record with `matching_method = 'SEMANTIC'`, `semantic_score = 0.9450`, and `model_version = 'all-MiniLM-L6-v2'` is **never read, used, or overwritten**. The deterministic engine creates its distinct record with `matching_method = 'CANONICAL_SKILL_COVERAGE'`, `algorithm_version = 'deterministic-v1'`, preserving historical ML data intact.
10. **Deterministic Repeated Evaluation Oracle**: 1,000 evaluations across multiple threads yield identical scores with zero variance ($\sigma^2 = 0$). Deterministic comparison asserts identical `overall_score`, `required_skill_coverage`, `preferred_skill_coverage`, matched/missing counts, ordered `match_skill_details`, and explanation content; generated IDs, `scored_at`/`evaluated_at` timestamps, and runtime metadata are explicitly excluded from equality assertions.
11. **Duplicate Skills Deduplication Oracle**: Repeated skills in job or candidate sets are deduplicated to single canonical elements before scoring.
12. **Requirement Overlap Resolution Oracle**: If a skill is listed as both Required and Preferred, it is assigned to Required and purged from Preferred.
13. **Stale-Result Invalidation Oracle**: Any job mutation that can change the extracted/matched requirement set invalidates existing deterministic match results. Updating job title, description, or skill requirements via `JobService.updateJob()` atomically marks affected match results with `is_stale = true`. Re-running matching recomputes scores and resets `is_stale = false`.
14. **Resource Authorization Boundary Oracle**: A recruiter attempting to match or inspect a job or resume outside their authorized department/tenant receives `HTTP 403 FORBIDDEN` (`UNAUTHORIZED_RESOURCE_ACCESS`). Cross-tenant matching is strictly blocked.
15. **Batch Size Limit (>100) Oracle**: A match request spanning 105 candidates is rejected immediately prior to evaluation with `HTTP 422 UNPROCESSABLE ENTITY` (`BATCH_SIZE_LIMIT_EXCEEDED`). Zero database writes occur.

---

## 23. Risks

| ID | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| R-01 | `match_results` UQ constraint prevents upsert | LOW | HIGH | Phase 4 writes `matching_method = 'CANONICAL_SKILL_COVERAGE'` with V5 CHECK expansion; cleanly coexists with legacy ML rows under existing unique constraint |
| R-02 | Job description yields 0 canonical skills | MEDIUM | HIGH | Return HTTP 422 `MATCHING_REQUIREMENTS_NOT_FOUND` |
| R-03 | `parsed_years_experience` unreliable (heuristic) | HIGH | LOW | Use as informational display only; not scored |
| R-04 | Existing job records have naive String.contains() job_skills | LOW | LOW | Must re-extract after Phase 4 upgrade |
| R-05 | Different extraction results for job vs. resume same text | LOW | MEDIUM | Both use same DeterministicSkillExtractor |
| R-06 | V5 migration conflicts with Flyway state | LOW | HIGH | V5 is additive-only; no V3 modification |
| R-07 | Candidate PII leaking through explanation evidence | MEDIUM | HIGH | Architecture B: DeterministicPiiScrubber before persistence |

---

## 24. Open Questions — Resolutions (All OQ-01 through OQ-08 Resolved)

| ID | Question | Status | Formal Decision & Rationale |
|---|---|---|---|
| **OQ-01** | Behavior when job description yields 0 canonical skills? | **RESOLVED** | **A zero-requirement job is unmatchable.** Return `HTTP 422 UNPROCESSABLE ENTITY` with error code `MATCHING_REQUIREMENTS_NOT_FOUND`. Prevents misleading 100% or default matches. |
| **OQ-02** | Weight allocation between Required and Preferred? | **RESOLVED** | **Baseline 0.80 Required / 0.20 Preferred approved.** If only required skills exist, $w_{req} = 1.00$. If only preferred skills exist, $w_{pref} = 1.00$. |
| **OQ-03** | Continuous confidence weighting vs Binary presence? | **RESOLVED** | **Binary Presence (Option A) approved.** Each matched skill = 1.0. Confidence is preserved strictly as informational evidence metadata for explainability. |
| **OQ-04** | Role of `minimum_years` experience in scoring? | **RESOLVED** | **Excluded from Phase 4 scoring.** `minimum_years` is informational display metadata only. Experience scoring is deferred to a separately specified engine. |
| **OQ-05** | Add `location`, `employment_type`, `remote_policy` to `jobs` in V5? | **RESOLVED** | **Approved.** Additive columns included in Flyway migration `V5`. |
| **OQ-06** | Batch matching vs Single candidate matching lifecycle? | **RESOLVED** | **Explicit recruiter-triggered matching.** Job creation does NOT trigger batch evaluation. Matching is triggered via explicit `POST /api/v1/jobs/{id}/matches`. |
| **OQ-07** | Fate of legacy `/api/matching` controller? | **RESOLVED** | **Deprecated.** Replaced by `/api/v1/jobs/{id}/matches`. All legacy SBERT integration decoupled. |
| **OQ-08** | Handling of historical SBERT `match_results` rows? | **RESOLVED** | **Retain in database as historical audit records.** Deterministic engine writes with `matching_method = 'CANONICAL_SKILL_COVERAGE'` and `algorithm_version = 'deterministic-v1'`, completely isolated from ML scores. |

---

## 25. Recommended Implementation Sequence

This sequence is advisory. Formal planning begins after architecture approval.

| Step | Task | Dependencies |
|---|---|---|
| 1 | Author and apply `V5__phase4_matching_schema.sql` | Approval of OQ-05, OQ-08 |
| 2 | Create `MatchSkillDetail` entity + `MatchSkillDetailRepository` | V5 applied |
| 3 | Extend `JobSkill` entity with evidence fields | V5 applied |
| 4 | Extend `MatchResult` entity with Phase 4 score fields | V5 applied |
| 5 | Create DTOs: `MatchResponseDto`, `MatchSkillDetailDto`, `JobSkillDto` | None |
| 6 | Replace `JobService.createJob()` skill extraction with `DeterministicSkillExtractor` | None |
| 7 | Create `SkillCoverageMatchingService` (core Phase 4 engine) | Steps 2-5 |
| 8 | Replace `MatchingController` with Phase 4 controller at `/api/v1/` | Step 7 |
| 9 | Fix `JobController` path prefix to `/api/v1/jobs` | None |
| 10 | Write unit tests (determinism, edge cases, fairness assertions) | Step 7 |
| 11 | Write integration tests | Steps 7-9 |
| 12 | Write API tests | Steps 8-9 |
| 13 | Full regression: mvn test -> 75+ pre-existing + new Phase 4 tests passing | Steps 10-12 |
| 14 | Phase 4 adversarial revalidation | Step 13 |

---

## Summary: What Phase 4 Is and Is Not

| IS | IS NOT |
|---|---|
| Deterministic skill-coverage matching | SBERT/semantic embedding matching |
| Explainable decomposed score | Single opaque percentage |
| Canonical skill set intersection | Keyword frequency / TF-IDF |
| Per-skill evidence linking | Black-box ML ranking |
| Audit-logged match computation | Automated hiring decisions |
| Fairness-constrained by architecture: protected attributes and identified proxy features are excluded from scoring, and the scoring function is deterministic and auditable | Bias mitigation algorithm / Universal fairness proof |
| Reproducible given same inputs | Probabilistic or random |

---

```
================================================================================
  PHASE 4 STATUS: ARCHITECTURE DESIGN COMPLETE — AWAITING APPROVAL
================================================================================
```
