# FairHire AI — Comprehensive Domain Model & Schema Design Specification

> **Phase 2A Architecture Deliverable (Revision Pass)**  
> **Status:** Proposed & Audited (Design Only — Implementation Pending Review)  
> **Target Database Engine:** PostgreSQL 16 (Canonical Docker Development & Production)  
> **Migration Framework:** Flyway (`V1__...` planned for Phase 2B)

---

## 1. Audit of Existing Inherited JPA Models

Prior to Phase 2, the repository contained 10 preliminary JPA models (`backend/src/main/java/com/fairhire/models/`). An exhaustive audit was conducted to identify flaws, research shortcomings, and design gaps.

| Inherited Model | Current Fields & Types | Current Relationships | Current Assumptions & Problems | Architectural Verdict & Action |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`User`** | `id` (Long, PK)<br>`name` (varchar 120)<br>`email` (varchar 120, unique)<br>`password` (varchar 255)<br>`role` (varchar 50, default "recruiter")<br>`createdAt` (timestamp) | None mapped | • Roles are untyped arbitrary strings.<br>• No `updated_at` or audit timestamps.<br>• No user `status` (ACTIVE/INACTIVE).<br>• `Job.createdBy` references `User.id` as a loose scalar `Long` without an FK constraint. | **Redesign**: Retain core fields, add role Enum (`RECRUITER`, `ADMIN`, `CANDIDATE`), add `updated_at`, add `status`, enforce foreign keys to created jobs, experiment runs, and audit logs. |
| **`Job`** | `id` (Long, PK)<br>`createdBy` (Long)<br>`title` (varchar 200)<br>`description` (TEXT)<br>`requirements` (TEXT)<br>`createdAt` (timestamp) | `@OneToMany` JobSkill<br>`@OneToMany` BiasReport | • `createdBy` is an unconstrained Long with no FK to `users`.<br>• `requirements` is an unstructured text block with no machine-readable qualification schema.<br>• No `status` (DRAFT, ACTIVE, ARCHIVED).<br>• No `department` or `location`.<br>• Bias reports are attached without tracking original vs rewritten JD state. | **Redesign**: Add FK to `users(id)`. Extract structured qualifications into `JobRequirement`. Add `department`, `status`, `updated_at`, and explicit association to `RewriteSuggestion`. |
| **`Candidate`** | `id` (Long, PK)<br>`name` (varchar 120)<br>`email` (varchar 120)<br>`createdAt` (timestamp) | Referenced by `Resume` (`@ManyToOne`) | • Completely decoupled from `User` even when a candidate registers an account.<br>• No phone number, contact metadata, or candidate status.<br>• Must strictly never contain inferred demographic labels. | **Redesign**: Add optional FK to `users(id)`. Add phone, headline, `created_at`, `updated_at`. Protected attributes are strictly excluded from candidate application records. |
| **`Resume`** | `id` (Long, PK)<br>`candidate_id` (FK Candidate)<br>`filePath` (varchar 255)<br>`rawText` (TEXT)<br>`anonymizedText` (TEXT)<br>`yearsExperience` (Double)<br>`education` (TEXT)<br>`createdAt` (timestamp) | `@ManyToOne` Candidate<br>`@OneToMany` ResumeSkill | • Stores `anonymizedText` directly on the resume row without recording what was redacted, how many redactions occurred, or redaction rules used.<br>• Missing file cryptographic hash (SHA-256) for file integrity & duplicate detection.<br>• Missing parsing status (PENDING, PARSED, FAILED). | **Redesign**: Add `file_hash_sha256`, `file_type` (PDF, DOCX, TXT), `parsing_status`, `parsed_at`. Extract blind screening into a first-class `BlindScreeningResult` entity with redaction change-logs. |
| **`Skill`** | `id` (Long, PK)<br>`skillName` (varchar 100, unique)<br>`category` (varchar 100) | Referenced by `JobSkill`, `ResumeSkill` | • Basic dictionary table is functional.<br>• Lacks alias / synonym mapping (e.g., "Postgres" vs "PostgreSQL"). | **Retain & Extend**: Retain normalized structure; add `synonyms` array support for robust ATS keyword matching. |
| **`JobSkill`** | `id` (Long, PK)<br>`job_id` (FK Job)<br>`skill_id` (FK Skill) | Many-to-one to Job, Skill | • Simple join table.<br>• Lacks requirement weighting (e.g., mandatory vs preferred) and minimum required years of experience for the skill. | **Redesign**: Add `is_mandatory` (boolean), `minimum_years` (integer), and `weight` (decimal). Delineate clearly from `JobRequirement`. |
| **`ResumeSkill`**| `id` (Long, PK)<br>`resume_id` (FK Resume)<br>`skill_id` (FK Skill) | Many-to-one to Resume, Skill | • Simple join table.<br>• Lacks skill extraction confidence score and context location (e.g., found in Summary vs Work History). | **Redesign**: Add `extraction_confidence` (float 0.0–1.0), `years_experience` (float), and `context_snippet`. |
| **`Match`** | `id` (Long, PK)<br>`job_id` (FK Job)<br>`resume_id` (FK Resume)<br>`keywordScore` (Double)<br>`semanticScore` (Double)<br>`finalScore` (Double)<br>`normalRank` (Integer)<br>`blindRank` (Integer)<br>`createdAt` (timestamp) | `@ManyToOne` Job<br>`@ManyToOne` Resume | **CRITICAL RESEARCH DEFECT**: Conflates multiple screening modes and runs into one record.<br>• Only stores two hardcoded scores (`keywordScore`, `semanticScore`).<br>• Normal rank and blind rank are placed side-by-side, but blind screening semantic score is not stored separately!<br>• Weights $w_{kw}, w_{sem}$ used to compute `finalScore` are lost.<br>• Lacks direct link to `Candidate` for clear three-way traceability. | **Redesign**: Replace with `MatchResult`. Store explicit `screening_mode` (`NORMAL`, `BLIND`), `matching_method` (`KEYWORD`, `SEMANTIC`, `HYBRID`), weights, raw cosine similarities, computed final score, and pool rank. Traceable to Job, Candidate, and Resume. |
| **`BiasReport`**| `id` (Long, PK)<br>`job_id` (FK Job)<br>`phrase` (varchar 100)<br>`category` (varchar 50)<br>`severity` (varchar 50)<br>`suggestion` (varchar 255)<br>`createdAt` (timestamp) | `@ManyToOne` Job | • Only records token-level flags on a job.<br>• Conflates readability with bias without separate methodology.<br>• Cannot compare before vs after inclusive rewrite. | **Redesign**: Split into: (1) `BiasReport` containing aggregate JD-level scores with distinct bias metrics and readability metrics; (2) `RewriteSuggestion` tracking phrase-level replacements and user acceptance. |
| **`Experiment`**| `id` (Long, PK)<br>`type` (varchar 100)<br>`datasetName` (varchar 100)<br>`metricName` (varchar 100)<br>`metricValue` (Double)<br>`createdAt` (timestamp) | None | **CRITICAL RESEARCH DEFECT**: Completely unnormalized key-value store.<br>• No experiment run identity or UUID.<br>• No hyperparameter or model configuration tracking.<br>• No dataset version link or cryptographic reproducibility.<br>• No candidate-level rankings or rank shift tracking.<br>• Unusable for academic research. | **Redesign**: Replace entirely with Research Domain hierarchy: `DatasetVersion`, `DatasetRecord`, `Annotation`, `ExperimentRun`, `ExperimentResult`, `ExperimentMetric`. |

---

## 2. Ethical Principles & Demographic Data Governance

A fundamental tenet of FairHire AI is ethical algorithmic governance:

1. **Strict Prohibition of Inferred Protected Attributes:**
   - **FairHire AI must NEVER infer protected attributes (gender, age, ethnicity, disability, sexual orientation, religion, or caste) from names, resumes, photos, institutions, geographic locations, linguistic style, or any other proxies.**
   - Algorithmic profiling or classification of individuals into demographic categories is strictly prohibited in all application components.
2. **Isolation of Research Demographic Labels:**
   - Application domain entities (`User`, `Candidate`, `Resume`) **never** store or process demographic attributes.
   - Demographic labels are permitted **strictly and exclusively** within the offline Research Domain (`DatasetRecord.demographic_ground_truth`).
   - These research labels must be **explicitly sourced from authorized, self-reported benchmark research corpora** with documented consent (e.g., standard anonymized public academic datasets). They are used exclusively to evaluate whether algorithms exhibit disparate impact across demographic cohorts.

---

## 3. Application Domain Design

The Application Domain manages operational recruiting workflows, job postings, candidate resumes, bias auditing, and interactive screening.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        APPLICATION DOMAIN                             │
│                                                                        │
│   ┌──────────┐         ┌───────────────┐        ┌──────────────────┐   │
│   │   User   │1──────* │      Job      │1─────* │  JobRequirement  │   │
│   └──────────┘         └───────┬───────┘        └──────────────────┘   │
│                                │1                                      │
│                                ├───* ┌─────────────────────────────┐   │
│                                │     │          JobSkill           │   │
│                                │     └──────────────┬──────────────┘   │
│                                │1                   │*                 │
│                                ├───* ┌──────────────▼──────────────┐   │
│                                │     │            Skill            │   │
│                                │     └──────────────▲──────────────┘   │
│                                │1                   │*                 │
│                                ├───* ┌──────────────┴──────────────┐   │
│                                │     │         ResumeSkill         │   │
│                                │     └──────────────┬──────────────┘   │
│                                │                    │*                 │
│   ┌──────────┐1       1┌───────▼───────┐1          1│                  │
│   │Candidate │──────── │    Resume     │────────────┘                  │
│   └────┬─────┘         └───────┬───────┘                               │
│        │1                      │1                                      │
│        │                       ├───1 ┌─────────────────────────────┐   │
│        │                       │     │    BlindScreeningResult     │   │
│        │                       │     └─────────────────────────────┘   │
│        │*                      │*                                      │
│        └───────────┐   ┌───────┘                                       │
│                    ▼   ▼                                               │
│               ┌─────────────┐                                          │
│               │ MatchResult │                                          │
│               └─────────────┘                                          │
│                                                                        │
│   ┌───────────────────────────┐1       *┌──────────────────────────┐   │
│   │        BiasReport         │─────────│    RewriteSuggestion     │   │
│   └───────────────────────────┘         └──────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────┘
```

### Distinction: `JobRequirement` vs. `JobSkill`

To eliminate ambiguity and redundancy:
- **`JobRequirement` (Structured High-Level Qualifications):**  
  Represents qualitative, broad, or policy-based job specifications. Examples include degree level (`"Bachelor's in Computer Science"`), overall industry experience (`"5+ years professional software engineering"`), certifications (`"AWS Certified Solutions Architect"`), and language proficiencies (`"Professional English fluency"`). It acts as a gatekeeper criteria set for recruiters.
- **`JobSkill` (Machine-Readable Taxonomy Mapping):**  
  Represents exact, normalized technical and domain skills explicitly mapped to the canonical `Skill` vocabulary catalog (e.g., skill ID pointing to `"Python"`, `"Docker"`, `"PostgreSQL"`). This is the machine-actionable entity utilized directly by the ATS keyword matching and Sentence-BERT semantic scoring engines.

### Entity Specifications

#### 1. `User`
- **Purpose**: System accounts for recruiters, research administrators, and applicants.
- **Fields**:
  - `id`: BIGSERIAL, PK.
  - `name`: VARCHAR(120), NOT NULL.
  - `email`: VARCHAR(120), NOT NULL, UNIQUE.
  - `password_hash`: VARCHAR(255), NOT NULL.
  - `role`: VARCHAR(30), NOT NULL (`RECRUITER`, `CANDIDATE`, `ADMIN`).
  - `status`: VARCHAR(20), NOT NULL DEFAULT `'ACTIVE'` (`ACTIVE`, `INACTIVE`, `SUSPENDED`).
  - `created_at`: TIMESTAMPTZ, NOT NULL DEFAULT `CURRENT_TIMESTAMP`.
  - `updated_at`: TIMESTAMPTZ, NOT NULL DEFAULT `CURRENT_TIMESTAMP`.

#### 2. `Job`
- **Purpose**: Requisitions and job descriptions created by recruiters.
- **Fields**:
  - `id`: BIGSERIAL, PK.
  - `creator_id`: BIGINT, NOT NULL, FK $\to$ `users(id)` ON DELETE RESTRICT.
  - `title`: VARCHAR(200), NOT NULL.
  - `department`: VARCHAR(100), NULL.
  - `raw_description`: TEXT, NOT NULL.
  - `rewritten_description`: TEXT, NULL (sanitized, inclusive version).
  - `status`: VARCHAR(30), NOT NULL DEFAULT `'ACTIVE'` (`DRAFT`, `ACTIVE`, `CLOSED`, `ARCHIVED`).
  - `created_at`: TIMESTAMPTZ, NOT NULL DEFAULT `CURRENT_TIMESTAMP`.
  - `updated_at`: TIMESTAMPTZ, NOT NULL DEFAULT `CURRENT_TIMESTAMP`.

#### 3. `JobRequirement`
- **Purpose**: High-level qualitative and structural requirements for a job requisition.
- **Fields**:
  - `id`: BIGSERIAL, PK.
  - `job_id`: BIGINT, NOT NULL, FK $\to$ `jobs(id)` ON DELETE CASCADE.
  - `requirement_type`: VARCHAR(50), NOT NULL (`EXPERIENCE_YEARS`, `EDUCATION_LEVEL`, `CERTIFICATION`, `LANGUAGE`, `GENERAL`).
  - `requirement_value`: VARCHAR(255), NOT NULL (e.g., "5+ Years", "Master's Degree", "PMP").
  - `necessity`: VARCHAR(20), NOT NULL DEFAULT `'REQUIRED'` (`REQUIRED`, `PREFERRED`).
  - `weight`: NUMERIC(4, 3), NOT NULL DEFAULT `1.000`.

#### 4. `Candidate`
- **Purpose**: Real-world job applicant identity. Strictly free of inferred protected demographic attributes.
- **Fields**:
  - `id`: BIGSERIAL, PK.
  - `user_id`: BIGINT, NULL, FK $\to$ `users(id)` ON DELETE SET NULL (for registered applicants).
  - `full_name`: VARCHAR(150), NOT NULL.
  - `email`: VARCHAR(150), NOT NULL, UNIQUE.
  - `phone`: VARCHAR(50), NULL.
  - `created_at`: TIMESTAMPTZ, NOT NULL DEFAULT `CURRENT_TIMESTAMP`.
  - `updated_at`: TIMESTAMPTZ, NOT NULL DEFAULT `CURRENT_TIMESTAMP`.

#### 5. `Resume`
- **Purpose**: Uploaded candidate resume document and extracted plain-text representation.
- **Fields**:
  - `id`: BIGSERIAL, PK.
  - `candidate_id`: BIGINT, NOT NULL, FK $\to$ `candidates(id)` ON DELETE CASCADE.
  - `file_path`: VARCHAR(500), NULL (file storage path / volume URI).
  - `file_name`: VARCHAR(255), NOT NULL.
  - `file_type`: VARCHAR(20), NOT NULL (`PDF`, `DOCX`, `TXT`).
  - `file_hash_sha256`: CHAR(64), NOT NULL (cryptographic document hash for integrity).
  - `raw_text`: TEXT, NOT NULL.
  - `parsed_years_experience`: NUMERIC(4, 1), NOT NULL DEFAULT `0.0`.
  - `parsed_education`: TEXT, NULL.
  - `parsing_status`: VARCHAR(30), NOT NULL DEFAULT `'COMPLETED'` (`PENDING`, `COMPLETED`, `FAILED`).
  - `created_at`: TIMESTAMPTZ, NOT NULL DEFAULT `CURRENT_TIMESTAMP`.
  - `updated_at`: TIMESTAMPTZ, NOT NULL DEFAULT `CURRENT_TIMESTAMP`.

#### 6. `Skill`
- **Purpose**: Normalized vocabulary of technical, functional, and domain skills.
- **Fields**:
  - `id`: BIGSERIAL, PK.
  - `name`: VARCHAR(100), NOT NULL, UNIQUE.
  - `category`: VARCHAR(50), NOT NULL (e.g., `LANGUAGES`, `FRAMEWORKS`, `DATABASE`, `CLOUD`, `DATA_SCIENCE_AI`).
  - `synonyms`: TEXT[], NULL (PostgreSQL array of aliases, e.g., `{"postgresql", "postgres", "psql"}`).
  - `created_at`: TIMESTAMPTZ, NOT NULL DEFAULT `CURRENT_TIMESTAMP`.

#### 7. `JobSkill`
- **Purpose**: Machine-readable skill taxonomy mapping linking `Job` and `Skill` for scoring algorithms.
- **Fields**:
  - `id`: BIGSERIAL, PK.
  - `job_id`: BIGINT, NOT NULL, FK $\to$ `jobs(id)` ON DELETE CASCADE.
  - `skill_id`: BIGINT, NOT NULL, FK $\to$ `skills(id)` ON DELETE RESTRICT.
  - `is_mandatory`: BOOLEAN, NOT NULL DEFAULT `TRUE`.
  - `minimum_years`: INTEGER, NOT NULL DEFAULT `0`.
  - `weight`: NUMERIC(4, 3), NOT NULL DEFAULT `1.000`.
  - **Constraint**: `UNIQUE (job_id, skill_id)`.

#### 8. `ResumeSkill`
- **Purpose**: Normalized skills extracted from a candidate's resume.
- **Fields**:
  - `id`: BIGSERIAL, PK.
  - `resume_id`: BIGINT, NOT NULL, FK $\to$ `resumes(id)` ON DELETE CASCADE.
  - `skill_id`: BIGINT, NOT NULL, FK $\to$ `skills(id)` ON DELETE RESTRICT.
  - `extraction_confidence`: NUMERIC(4, 3), NOT NULL DEFAULT `1.000`.
  - `context_snippet`: VARCHAR(300), NULL.
  - **Constraint**: `UNIQUE (resume_id, skill_id)`.

#### 9. `BlindScreeningResult`
- **Purpose**: Sanitized version of a resume stripped of explicit identifiers, with a redaction log.
- **Methodological Scope & Limitations**:
  - **Explicit Identifiers Stripped:** Candidate name, honorifics/titles (`Mr.`, `Ms.`), gender-identifying pronouns (`he/him`, `she/her`), contact information (email, phone), and explicit demographic header lines (`Gender: Female`, `DOB: ...`).
  - **Proxy Limitation:** Blind screening removes explicit demographic tokens, but **cannot guarantee the elimination of all latent demographic proxies** (e.g., historically single-gender institutions, cultural organizations, or graduation years implying age brackets).
  - **Research Objective:** The research framework evaluates the **ranking sensitivity and rank shift ($\Delta_{rank}$, $\rho_{Spearman}$)** resulting from explicit identity sanitization; it does not claim that blind screening inherently eliminates all algorithmic or cognitive bias.
- **Fields**:
  - `id`: BIGSERIAL, PK.
  - `resume_id`: BIGINT, NOT NULL, UNIQUE, FK $\to$ `resumes(id)` ON DELETE CASCADE.
  - `anonymized_text`: TEXT, NOT NULL.
  - `redactions_count`: INTEGER, NOT NULL DEFAULT `0`.
  - `redactions_log`: JSONB, NOT NULL (structured array: `[{type, original, replacement, position}]`).
  - `anonymization_ruleset_version`: VARCHAR(50), NOT NULL DEFAULT `'v1.0'`.
  - `created_at`: TIMESTAMPTZ, NOT NULL DEFAULT `CURRENT_TIMESTAMP`.

#### 10. `BiasReport`
- **Purpose**: Multi-dimensional linguistic evaluation of a Job Description. Explicitly separates fairness/bias metrics from readability metrics.
- **Distinct Metric Definitions**:
  1. **Fairness / Bias Metrics:**
     - `gender_bias_score` (Scale: $0.0000$ to $1.0000$): Magnitude of gendered language imbalance based on Gaucher et al. (2011) word banks. $0.0$ indicates balanced/neutral language.
     - `gender_lean` (Categorical): `BALANCED` (lean delta $\le 0.15$), `MASCULINE_SKEWED` (excess masculine terms), or `FEMININE_SKEWED` (excess feminine terms).
     - `age_bias_score` (Scale: $0.0000$ to $1.0000$): Density of exclusionary age-coded terminology relative to document word count.
  2. **Readability Metrics:**
     - `flesch_reading_ease` (Scale: $0.0$ to $100.0+$): Flesch reading ease formula based on sentence length and syllable count. Higher is easier to read.
     - `reading_level` (Categorical): `EASY` ($\ge 70$), `MODERATE` ($50 - 69$), `DIFFICULT` ($< 50$).
- **Fields**:
  - `id`: BIGSERIAL, PK.
  - `job_id`: BIGINT, NOT NULL, FK $\to$ `jobs(id)` ON DELETE CASCADE.
  - `target_version`: VARCHAR(20), NOT NULL DEFAULT `'ORIGINAL'` (`ORIGINAL`, `REWRITTEN`).
  - `gender_bias_score`: NUMERIC(5, 4), NOT NULL.
  - `gender_lean`: VARCHAR(30), NOT NULL.
  - `age_bias_score`: NUMERIC(5, 4), NOT NULL.
  - `flesch_reading_ease`: NUMERIC(5, 2), NOT NULL.
  - `reading_level`: VARCHAR(30), NOT NULL.
  - `total_flagged_terms`: INTEGER, NOT NULL DEFAULT `0`.
  - `masculine_terms_count`: INTEGER, NOT NULL DEFAULT `0`.
  - `feminine_terms_count`: INTEGER, NOT NULL DEFAULT `0`.
  - `age_terms_count`: INTEGER, NOT NULL DEFAULT `0`.
  - `created_at`: TIMESTAMPTZ, NOT NULL DEFAULT `CURRENT_TIMESTAMP`.

#### 11. `RewriteSuggestion`
- **Purpose**: Token-level inclusive replacement suggestions with recruiter acceptance status.
- **Fields**:
  - `id`: BIGSERIAL, PK.
  - `bias_report_id`: BIGINT, NOT NULL, FK $\to$ `bias_reports(id)` ON DELETE CASCADE.
  - `original_phrase`: VARCHAR(100), NOT NULL.
  - `suggested_replacement`: VARCHAR(100), NOT NULL.
  - `category`: VARCHAR(50), NOT NULL (`GENDER_MASCULINE`, `GENDER_FEMININE`, `AGE_EXCLUSIONARY`, `COMPLEX_READABILITY`).
  - `context_snippet`: VARCHAR(300), NOT NULL.
  - `is_accepted`: BOOLEAN, NOT NULL DEFAULT `TRUE`.
  - `created_at`: TIMESTAMPTZ, NOT NULL DEFAULT `CURRENT_TIMESTAMP`.

#### 12. `MatchResult`
- **Purpose**: Operational scoring record for recruiter decision-making. Fully traceable across Job, Candidate, and Resume.
- **Three-Way Traceability**: Directly references `job_id`, `candidate_id`, and `resume_id`.
- **Fields**:
  - `id`: BIGSERIAL, PK.
  - `job_id`: BIGINT, NOT NULL, FK $\to$ `jobs(id)` ON DELETE CASCADE.
  - `candidate_id`: BIGINT, NOT NULL, FK $\to$ `candidates(id)` ON DELETE CASCADE.
  - `resume_id`: BIGINT, NOT NULL, FK $\to$ `resumes(id)` ON DELETE CASCADE.
  - `screening_mode`: VARCHAR(20), NOT NULL (`NORMAL`, `BLIND`).
  - `matching_method`: VARCHAR(30), NOT NULL (`KEYWORD`, `SEMANTIC`, `HYBRID`).
  - `keyword_score`: NUMERIC(5, 4), NOT NULL DEFAULT `0.0`.
  - `skill_jaccard_score`: NUMERIC(5, 4), NOT NULL DEFAULT `0.0`.
  - `semantic_score`: NUMERIC(5, 4), NOT NULL DEFAULT `0.0`.
  - `keyword_weight`: NUMERIC(4, 3), NOT NULL DEFAULT `0.300`.
  - `semantic_weight`: NUMERIC(4, 3), NOT NULL DEFAULT `0.700`.
  - `final_composite_score`: NUMERIC(5, 4), NOT NULL.
  - `rank_in_pool`: INTEGER, NOT NULL.
  - `matched_skills_count`: INTEGER, NOT NULL DEFAULT `0`.
  - `missing_skills_count`: INTEGER, NOT NULL DEFAULT `0`.
  - `missing_skills`: JSONB, NULL (array of missing skill names).
  - `model_version`: VARCHAR(100), NOT NULL DEFAULT `'all-MiniLM-L6-v2'`.
  - `created_at`: TIMESTAMPTZ, NOT NULL DEFAULT `CURRENT_TIMESTAMP`.
  - **Constraint**: `UNIQUE (job_id, resume_id, screening_mode, matching_method)`.

#### 13. `AuditLog`
- **Purpose**: Tamper-proof audit trail for automated decision transparency and compliance.
- **Fields**:
  - `id`: BIGSERIAL, PK.
  - `user_id`: BIGINT, NULL, FK $\to$ `users(id)` ON DELETE SET NULL.
  - `action`: VARCHAR(100), NOT NULL (e.g., `MATCHING_TRIGGERED`, `BLIND_SCREENING_UNMASKED`, `JD_REWRITTEN`).
  - `entity_type`: VARCHAR(50), NOT NULL (`JOB`, `RESUME`, `EXPERIMENT`, `MATCH`).
  - `entity_id`: BIGINT, NOT NULL.
  - `details`: JSONB, NULL.
  - `ip_address`: VARCHAR(45), NULL.
  - `created_at`: TIMESTAMPTZ, NOT NULL DEFAULT `CURRENT_TIMESTAMP`.
- **Privacy & Compliance Policy**:
  - `ip_address` is stored strictly for security forensics and tracking unauthorized unmasking of candidate PII.
  - IP addresses are strictly operational metadata and are excluded from research datasets and evaluation metrics.
  - Research exports must programmatically sanitize and exclude audit-log PII.
  - Audit logs are subjected to a rolling 90-180 day retention policy with access restricted to administrators.

---

## 4. Research Domain Design

The Research Domain is fully decoupled from the operational application. It evaluates scientific hypotheses on immutable benchmark corpora.

```
┌────────────────────────────────────────────────────────────────────────┐
│                          RESEARCH DOMAIN                               │
│                                                                        │
│   ┌────────────────────┐1         *┌──────────────────────────────┐    │
│   │   DatasetVersion   │───────────│        DatasetRecord         │    │
│   └─────────┬──────────┘           └──────────────┬───────────────┘    │
│             │1                                    │1                   │
│             │                                     │*                   │
│             │                             ┌───────▼───────────────┐    │
│             │                             │      Annotation       │    │
│             │                             └───────────────────────┘    │
│             │1                                                         │
│             │*                                                         │
│   ┌─────────▼──────────┐1         *┌──────────────────────────────┐    │
│   │   ExperimentRun    │───────────│       ExperimentResult       │    │
│   └─────────┬──────────┘           └──────────────────────────────┘    │
│             │1                                                         │
│             │*                                                         │
│   ┌─────────▼──────────┐                                               │
│   │  ExperimentMetric  │                                               │
│   └────────────────────┘                                               │
└────────────────────────────────────────────────────────────────────────┘
```

### Dataset Lifecycle States

A dataset version follows a strict, deliberate administrative lifecycle:

$$\text{DRAFT} \xrightarrow{\text{Verification}} \text{VALIDATED} \xrightarrow{\text{Admin Publish}} \text{FROZEN} \xrightarrow{\text{Superseded}} \text{DEPRECATED}$$

- **`DRAFT`:** Dataset is being ingested, tokenized, and verified. Records and annotations may be updated or appended.
- **`VALIDATED`:** Record counts, cross-references, and inter-annotator agreements are validated; archive hash is calculated.
- **`FROZEN`:** Explicitly finalized and published by an administrator. **The dataset is now strictly immutable.** No records, annotations, or texts may be altered or deleted.
- **`DEPRECATED`:** Superseded by a newer release (e.g., `v1.2.0`). Historical experiment runs referencing this version remain readable for scientific audit, but new experiments should avoid it.

> **Key Rule:** An `ExperimentRun` may **ONLY** execute against datasets with `lifecycle_status = 'FROZEN'`. A dataset **never** freezes automatically; immutability requires deliberate administrative publication.

### Entity Specifications

#### 1. `DatasetVersion`
- **Fields**:
  - `id`: BIGSERIAL, PK.
  - `version_tag`: VARCHAR(50), NOT NULL, UNIQUE (e.g., `'v1.0.0-academic'`, `'v1.1.0-annotated'`).
  - `name`: VARCHAR(150), NOT NULL.
  - `description`: TEXT, NOT NULL.
  - `checksum_sha256`: CHAR(64), NOT NULL (cryptographic SHA-256 hash of the complete dataset archive).
  - `total_resumes_count`: INTEGER, NOT NULL.
  - `total_jds_count`: INTEGER, NOT NULL.
  - `lifecycle_status`: VARCHAR(30), NOT NULL DEFAULT `'DRAFT'` (`DRAFT`, `VALIDATED`, `FROZEN`, `DEPRECATED`).
  - `frozen_at`: TIMESTAMPTZ, NULL (timestamp when explicitly published to FROZEN).
  - `created_at`: TIMESTAMPTZ, NOT NULL DEFAULT `CURRENT_TIMESTAMP`.

#### 2. `DatasetRecord`
- **Purpose**: An atomic candidate resume and job description evaluation pair within a specific dataset version.
- **Demographic Isolation**: `demographic_ground_truth` may **only** store explicitly sourced, self-reported benchmark labels from authorized research corpora.
- **Fields**:
  - `id`: BIGSERIAL, PK.
  - `dataset_version_id`: BIGINT, NOT NULL, FK $\to$ `dataset_versions(id)` ON DELETE RESTRICT.
  - `record_identifier`: VARCHAR(100), NOT NULL (e.g., `'PAIR-00142'`).
  - `jd_title`: VARCHAR(200), NOT NULL.
  - `jd_text`: TEXT, NOT NULL.
  - `jd_required_skills`: TEXT[], NOT NULL.
  - `candidate_identifier`: VARCHAR(100), NOT NULL (e.g., `'BENCH-CAND-0089'`).
  - `candidate_raw_resume`: TEXT, NOT NULL.
  - `candidate_anonymized_resume`: TEXT, NOT NULL.
  - `candidate_skills`: TEXT[], NOT NULL.
  - `demographic_ground_truth`: JSONB, NULL (explicitly sourced labels, e.g., `{source: "kaggle-diverse-v1", self_reported_gender: "F"}`).
  - `created_at`: TIMESTAMPTZ, NOT NULL DEFAULT `CURRENT_TIMESTAMP`.
  - **Constraint**: `UNIQUE (dataset_version_id, record_identifier)`.

#### 3. `Annotation`
- **Purpose**: Human expert ground-truth labels for relevance and bias benchmarking.
- **Fields**:
  - `id`: BIGSERIAL, PK.
  - `dataset_record_id`: BIGINT, NOT NULL, FK $\to$ `dataset_records(id)` ON DELETE CASCADE.
  - `annotator_id`: VARCHAR(100), NOT NULL.
  - `relevance_score`: INTEGER, NOT NULL (ordinal scale: 0 = Irrelevant, 1 = Marginally Relevant, 2 = Relevant, 3 = Highly Relevant).
  - `is_match`: BOOLEAN, NOT NULL (binary hiring recommendation threshold $\ge 2$).
  - `exclusionary_phrases_labeled`: JSONB, NULL.
  - `confidence`: NUMERIC(3, 2), NOT NULL DEFAULT `1.00`.
  - `notes`: TEXT, NULL.
  - `created_at`: TIMESTAMPTZ, NOT NULL DEFAULT `CURRENT_TIMESTAMP`.
  - **Constraint**: `UNIQUE (dataset_record_id, annotator_id)`.

#### 4. `ExperimentRun`
- **Purpose**: Unique, reproducible execution instance of an evaluation pipeline.
- **Primary Key**: `id UUID PK DEFAULT gen_random_uuid()` (guarantees global uniqueness across distributed benchmark runs).
- **Mandatory Reproducibility Tuple**:
  - `dataset_version_id` (FK to frozen dataset)
  - `method` (`KEYWORD_TFIDF`, `SEMANTIC_SBERT`, `HYBRID`, `BLIND_SEMANTIC`)
  - `embedding_model` & `model_version`
  - `preprocessing_version`
  - `anonymization_version`
  - `hyperparameters` JSONB
  - `git_commit_hash`
- **Fields**:
  - `id`: UUID, PK, DEFAULT `gen_random_uuid()`.
  - `run_name`: VARCHAR(150), NOT NULL.
  - `dataset_version_id`: BIGINT, NOT NULL, FK $\to$ `dataset_versions(id)` ON DELETE RESTRICT.
  - `executor_id`: BIGINT, NULL, FK $\to$ `users(id)` ON DELETE SET NULL.
  - `method`: VARCHAR(50), NOT NULL.
  - `embedding_model`: VARCHAR(100), NOT NULL DEFAULT `'all-MiniLM-L6-v2'`.
  - `model_version`: VARCHAR(50), NOT NULL DEFAULT `'1.0.0'`.
  - `preprocessing_version`: VARCHAR(50), NOT NULL DEFAULT `'v1.0-standard'`.
  - `anonymization_version`: VARCHAR(50), NOT NULL DEFAULT `'v1.0-ruleset'`.
  - `git_commit_hash`: VARCHAR(40), NULL (exact codebase version during execution).
  - `hyperparameters`: JSONB, NOT NULL (stores `{keyword_weight, semantic_weight, top_k, threshold, distance_metric}`).
  - `status`: VARCHAR(30), NOT NULL DEFAULT `'STARTED'` (`STARTED`, `COMPLETED`, `FAILED`).
  - `started_at`: TIMESTAMPTZ, NOT NULL DEFAULT `CURRENT_TIMESTAMP`.
  - `completed_at`: TIMESTAMPTZ, NULL.
  - `duration_ms`: BIGINT, NULL.
  - `error_message`: TEXT, NULL.

#### 5. `ExperimentResult`
- **Purpose**: Candidate-level evaluation record produced during an experiment run.
- **Fields**:
  - `id`: BIGSERIAL, PK.
  - `experiment_run_id`: UUID, NOT NULL, FK $\to$ `experiment_runs(id)` ON DELETE CASCADE.
  - `dataset_record_id`: BIGINT, NOT NULL, FK $\to$ `dataset_records(id)` ON DELETE RESTRICT.
  - `normal_score`: NUMERIC(6, 5), NOT NULL.
  - `blind_score`: NUMERIC(6, 5), NOT NULL.
  - `normal_rank`: INTEGER, NOT NULL.
  - `blind_rank`: INTEGER, NOT NULL.
  - `rank_change`: INTEGER NOT NULL, GENERATED ALWAYS AS (ABS(normal_rank - blind_rank)) STORED.
  - `ground_truth_relevance`: INTEGER, NULL.
  - `created_at`: TIMESTAMPTZ, NOT NULL DEFAULT `CURRENT_TIMESTAMP`.
  - **Constraint**: `UNIQUE (experiment_run_id, dataset_record_id)`.

#### 6. `ExperimentMetric`
- **Purpose**: Aggregate cohort-level research metrics calculated across all records in an experiment run.
- **Defined Metric Methodologies & Scales**:
  - `spearman_rho`: Spearman rank correlation coefficient ($\rho \in [-1, 1]$) between normal and blind screening rankings. Measures ranking stability under identity sanitization.
  - `mean_rank_shift`: Average absolute positional shift $\frac{1}{N}\sum |\text{rank}_{norm} - \text{rank}_{blind}|$.
  - `precision_at_k`: Fraction of top-$K$ candidates who meet relevance threshold ($\ge 2$ in ground-truth `Annotation`).
  - `recall_at_k`: Fraction of all ground-truth relevant candidates captured in the top-$K$.
  - `ndcg_at_k`: Normalized Discounted Cumulative Gain at rank $K$ ($0.0000$ to $1.0000$), measuring graded relevance ranking quality.
  - `f1_score`: Harmonic mean of Precision@K and Recall@K.
  - `jd_bias_score_before` & `jd_bias_score_after`: Aggregate linguistic bias score of the benchmark JD before and after inclusive rewriting.
  - `bias_reduction_delta`: Difference $\text{Score}_{before} - \text{Score}_{after}$.
- **Fields**:
  - `id`: BIGSERIAL, PK.
  - `experiment_run_id`: UUID, NOT NULL, UNIQUE, FK $\to$ `experiment_runs(id)` ON DELETE CASCADE.
  - `spearman_rho`: NUMERIC(6, 5), NOT NULL.
  - `mean_rank_shift`: NUMERIC(6, 3), NOT NULL.
  - `candidates_with_rank_change_count`: INTEGER, NOT NULL.
  - `precision_at_k`: NUMERIC(5, 4), NOT NULL.
  - `recall_at_k`: NUMERIC(5, 4), NOT NULL.
  - `ndcg_at_k`: NUMERIC(5, 4), NOT NULL.
  - `f1_score`: NUMERIC(5, 4), NOT NULL.
  - `jd_bias_score_before`: NUMERIC(5, 4), NOT NULL.
  - `jd_bias_score_after`: NUMERIC(5, 4), NOT NULL.
  - `bias_reduction_delta`: NUMERIC(5, 4), NOT NULL.
  - `eval_latency_avg_ms`: NUMERIC(8, 2), NOT NULL.
  - `created_at`: TIMESTAMPTZ, NOT NULL DEFAULT `CURRENT_TIMESTAMP`.

---

## 5. Future Exploratory Research: Composite Fairness Index

In earlier concept notes, a single "Composite Fairness Index" ($0 - 100$) was proposed to combine rank correlation, bias reduction, and readability into one number.

**Architectural Revision:**  
The Composite Fairness Index is **removed** as a core or mandatory database metric. Because arbitrary linear combinations of disparate metrics (rank correlation + linguistic term count delta + Flesch readability) lack established academic consensus, it is categorized as an **Exploratory Future Research Metric**. 

Prior to any future implementation, it must satisfy:
1. Rigorous mathematical formulation with sensitivity analysis.
2. Controlled empirical validation across benchmark datasets.
3. Explicit peer-reviewed or guide-approved methodological justification.

For Phase 2, evaluation relies strictly on established, mathematically sound metrics: $\rho_{Spearman}$, Mean Rank Shift, $\text{Precision}@K$, $\text{Recall}@K$, and $\text{NDCG}@K$.

---

## 6. Relationships & Integrity Constraints

### 1. One-to-One Relationships
- `Resume (1) <---> (1) BlindScreeningResult`: Every parsed resume has exactly one associated anonymized representation.
- `ExperimentRun (1) <---> (1) ExperimentMetric`: Every completed experiment run yields exactly one aggregate metric record.

### 2. One-to-Many Relationships
- `User (1) <---> (*) Job`: A recruiter creates multiple job descriptions.
- `User (1) <---> (*) AuditLog`: A user generates multiple audit events.
- `User (1) <---> (*) ExperimentRun`: A researcher executes multiple experiment runs.
- `Candidate (1) <---> (*) Resume`: A candidate can submit updated resume versions over time.
- `Candidate (1) <---> (*) MatchResult`: Candidate outcomes are directly traceable.
- `Job (1) <---> (*) JobRequirement`: A job requisition specifies multiple structured requirements.
- `Job (1) <---> (*) BiasReport`: A job has multiple bias assessments (e.g., original vs rewritten).
- `BiasReport (1) <---> (*) RewriteSuggestion`: A bias report yields multiple token replacement suggestions.
- `Job (1) <---> (*) MatchResult`: A job is matched against multiple candidate resumes.
- `Resume (1) <---> (*) MatchResult`: A resume is matched across multiple jobs or screening modes.
- `DatasetVersion (1) <---> (*) DatasetRecord`: A dataset version contains hundreds of benchmark pairs.
- `DatasetVersion (1) <---> (*) ExperimentRun`: A frozen dataset version serves as the basis for multiple experiments.
- `DatasetRecord (1) <---> (*) Annotation`: A dataset record may have labels from multiple human annotators.
- `ExperimentRun (1) <---> (*) ExperimentResult`: A run produces candidate-level results for every dataset record.

### 3. Many-to-Many Relationships
- `Job (*) <---> (*) Skill` via `JobSkill` (machine-readable skill mappings with mandatory flags, minimum years, weights).
- `Resume (*) <---> (*) Skill` via `ResumeSkill` (extracted candidate skills with confidence and context snippets).

---

## 7. Performance & Indexing Strategy

```sql
-- 1. Authentication & Candidate Lookups
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_candidates_email ON candidates(email);

-- 2. Resume Duplicate & Lookup Acceleration
CREATE INDEX idx_resumes_candidate_id ON resumes(candidate_id);
CREATE INDEX idx_resumes_file_hash ON resumes(file_hash_sha256);

-- 3. Operational Matching & Traceability
CREATE INDEX idx_job_skills_job_id ON job_skills(job_id);
CREATE INDEX idx_resume_skills_resume_id ON resume_skills(resume_id);
CREATE INDEX idx_match_results_traceability ON match_results(job_id, candidate_id, resume_id);
CREATE INDEX idx_match_results_lookup ON match_results(job_id, screening_mode, matching_method);
CREATE INDEX idx_match_results_scores ON match_results(job_id, rank_in_pool);

-- 4. Bias Auditing Lookups
CREATE INDEX idx_bias_reports_job_version ON bias_reports(job_id, target_version);
CREATE INDEX idx_rewrite_suggestions_report ON rewrite_suggestions(bias_report_id);

-- 5. Research Traceability & Benchmark Queries
CREATE INDEX idx_dataset_records_version ON dataset_records(dataset_version_id);
CREATE INDEX idx_annotations_record ON annotations(dataset_record_id);
CREATE INDEX idx_experiment_runs_dataset ON experiment_runs(dataset_version_id, method);
CREATE INDEX idx_experiment_results_run ON experiment_results(experiment_run_id);
CREATE INDEX idx_experiment_results_record ON experiment_results(dataset_record_id);

-- 6. Audit Logging Index
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
```
