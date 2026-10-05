# FairHire AI — Database Architecture & Schema Specification

**System Version:** 1.0.0 (Phase 2B Complete)  
**Database Engine:** PostgreSQL 16 (Canonical Database: `postgres:16-alpine`)  
**Migration Tool:** Flyway (`flyway-core`, `flyway-database-postgresql`)  
**JPA Provider:** Hibernate 6 (`spring.jpa.hibernate.ddl-auto=validate`)  

---

## 1. Executive Summary & Architectural Tenets

The FairHire AI database architecture is designed to support both a production-grade recruitment workflow and an empirical, peer-reviewed M.Sc. research framework. To achieve this dual objective without compromising either academic integrity or operational reliability, the database enforces three fundamental architectural tenets:

1. **Strict Domain Isolation:**
   - **Application Domain (13 tables):** Manages interactive recruitment operations—recruiter authentication, job specifications, candidate profiles, uploaded resumes, normalized skill taxonomies, bias diagnostics, blind screening redactions, composite matching results, and compliance audit logs.
   - **Research Domain (6 tables):** Manages frozen, reproducible benchmark datasets, atomic candidate–JD evaluation pairs, multi-annotator ground truth consensus, algorithmic experiment runs across 4 screening methods, pairwise evaluation results with rank-shift tracking, and aggregated statistical metrics.
   - **Zero Operational Cross-Pollination:** Operational candidate identifiers and live recruitment records never directly link to research benchmarks. Research pairs are decoupled via isolated identifiers (`record_identifier`, `candidate_identifier`), and operational match results (`match_results`) are stored separately from research experiment results (`experiment_results`).

2. **Ethical AI & Privacy Safeguards:**
   - **Demographic Isolation:** Demographic labels are restricted exclusively to `dataset_records.demographic_ground_truth` for offline algorithmic fairness auditing (disparate impact, demographic parity).
   - **Zero Protected Attribute Inference:** The system **never** infers, predicts, or derives demographic attributes (gender, race, age, religion, disability) from candidate names, resumes, educational institutions, dates, or linguistic proxies.
   - **Operational PII Protection:** Resumes support blind screening with auditable character-level redaction offsets in `blind_screening_results`.

3. **Deterministic Schema Management & Strict Validation:**
   - Every schema change is versioned using Flyway migrations located in `backend/src/main/resources/db/migration/`.
   - Hibernate runs in **`ddl-auto=validate`** mode across all environments. Hibernate is strictly forbidden from mutating tables (`update` or `create-drop` are banned in production/docker profiles). The database schema remains the single source of truth.

---

## 2. Global Conventions & Data Types

| Dimension | Standard Specification | Rationale |
| :--- | :--- | :--- |
| **Identifiers** | `BIGSERIAL` / `BIGINT` Primary Keys | High throughput, 64-bit space, standard sequential index clustering. |
| **Timestamps** | `TIMESTAMPTZ` (`TIMESTAMP WITH TIME ZONE`) | Timezone-aware auditability; ISO-8601 compatibility via Java `Instant`. |
| **Text Fields** | `VARCHAR(n)` for constrained strings, `TEXT` for unbounded bodies | Avoids `CHAR(n)` blank-padding (`bpchar`) discrepancies while enforcing length constraints. |
| **Arrays** | PostgreSQL native arrays (`TEXT[]`) | Efficient storage for skill tags and synonyms without relational join overhead. |
| **Structured Metadata** | PostgreSQL native JSONB (`JSONB`) | High-performance binary JSON indexing for audit logs, ground truth, and hyperparameter snapshots. |
| **Audit Trails** | `created_at` (`TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP`) | Immutable creation timestamps on all transactional tables. |

---

## 3. Data Dictionary — Application Domain

### 3.1. `users`
System accounts for recruiters, candidates, and research administrators.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Unique internal user ID |
| `name` | `VARCHAR(120)` | `NOT NULL` | User full name |
| `email` | `VARCHAR(120)` | `NOT NULL UNIQUE` | Authentication email |
| `password_hash` | `VARCHAR(255)` | `NOT NULL` | BCrypt password hash |
| `role` | `VARCHAR(30)` | `NOT NULL`, `CHECK (role IN ('RECRUITER', 'CANDIDATE', 'ADMIN'))` | Role-based authorization |
| `status` | `VARCHAR(20)` | `NOT NULL DEFAULT 'ACTIVE'`, `CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))` | Account lifecycle state |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Account creation timestamp |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Last profile update timestamp |

### 3.2. `jobs`
Job postings created by recruiters, containing raw textual descriptions and metadata.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Unique job ID |
| `creator_id` | `BIGINT` | `NOT NULL`, `FK -> users(id) ON DELETE RESTRICT` | Recruiter who posted the job |
| `title` | `VARCHAR(200)` | `NOT NULL` | Job position title |
| `department` | `VARCHAR(100)` | Nullable | Organization department |
| `description` | `TEXT` | `NOT NULL` | Original job description content |
| `rewritten_description` | `TEXT` | Nullable | Neutral/inclusive rewritten JD text |
| `status` | `VARCHAR(30)` | `NOT NULL DEFAULT 'ACTIVE'`, `CHECK (status IN ('DRAFT', 'ACTIVE', 'CLOSED', 'ARCHIVED'))` | Operational posting status |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Posting creation timestamp |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Last modification timestamp |

### 3.3. `job_requirements`
Structured qualifications and requirements parsed or authored for a job posting.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Unique requirement ID |
| `job_id` | `BIGINT` | `NOT NULL`, `FK -> jobs(id) ON DELETE CASCADE` | Associated job posting |
| `requirement_type` | `VARCHAR(50)` | `NOT NULL`, `CHECK (requirement_type IN ('EXPERIENCE_YEARS', 'EDUCATION_LEVEL', 'CERTIFICATION', 'LANGUAGE', 'GENERAL'))` | Qualification category |
| `requirement_value` | `VARCHAR(255)` | `NOT NULL` | Explicit requirement description |
| `necessity` | `VARCHAR(20)` | `NOT NULL DEFAULT 'REQUIRED'`, `CHECK (necessity IN ('REQUIRED', 'PREFERRED'))` | Requirement necessity |
| `weight` | `NUMERIC(3, 2)` | `NOT NULL DEFAULT 1.0 CHECK (weight >= 0.0 AND weight <= 1.0)` | Scoring weight factor |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Timestamp created |

### 3.4. `candidates`
Candidate operational identity and contact profile.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Unique candidate ID |
| `user_id` | `BIGINT` | Nullable, `FK -> users(id) ON DELETE SET NULL` | Linked platform user account |
| `full_name` | `VARCHAR(150)` | `NOT NULL` | Candidate legal full name |
| `email` | `VARCHAR(150)` | `NOT NULL UNIQUE` | Primary candidate email |
| `phone` | `VARCHAR(50)` | Nullable | Primary contact phone |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Candidate registration timestamp |

### 3.5. `resumes`
Uploaded resume document files and structured NLP-extracted textual content.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Unique resume ID |
| `candidate_id` | `BIGINT` | `NOT NULL`, `FK -> candidates(id) ON DELETE CASCADE` | Associated candidate profile |
| `file_path` | `VARCHAR(500)` | Nullable | Document storage path / URI |
| `file_name` | `VARCHAR(255)` | `NOT NULL` | Uploaded document file name |
| `file_type` | `VARCHAR(20)` | `NOT NULL`, `CHECK (file_type IN ('PDF', 'DOCX', 'TXT'))` | Document format MIME type |
| `file_hash_sha256` | `VARCHAR(64)` | `NOT NULL` | Cryptographic SHA-256 integrity hash |
| `raw_text` | `TEXT` | `NOT NULL` | Extracted plaintext body |
| `parsed_years_experience` | `NUMERIC(4, 1)`| `NOT NULL DEFAULT 0.0 CHECK (parsed_years_experience >= 0.0)` | Extracted total years of experience |
| `parsed_education` | `TEXT` | Nullable | Extracted degree/education summary |
| `parsing_status` | `VARCHAR(30)` | `NOT NULL DEFAULT 'COMPLETED'`, `CHECK (parsing_status IN ('PENDING', 'COMPLETED', 'FAILED'))` | Parser processing state |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Upload timestamp |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Last updated timestamp |

### 3.6. `skills`
Centralized, deduplicated skill taxonomy (e.g., Python, PostgreSQL, PyTorch, Docker).

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Unique canonical skill ID |
| `name` | `VARCHAR(100)` | `NOT NULL UNIQUE` | Canonical skill name |
| `category` | `VARCHAR(50)` | `NOT NULL` | Skill category (e.g., Programming, Cloud) |
| `synonyms` | `TEXT[]` | Nullable | Alternate terms / aliases |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Taxonomy creation timestamp |

### 3.7. `job_skills`
Many-to-many relationship mapping standardized skills to specific job descriptions.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Unique mapping ID |
| `job_id` | `BIGINT` | `NOT NULL`, `FK -> jobs(id) ON DELETE CASCADE` | Associated job posting |
| `skill_id` | `BIGINT` | `NOT NULL`, `FK -> skills(id) ON DELETE RESTRICT` | Canonical skill reference |
| `is_mandatory` | `BOOLEAN` | `NOT NULL DEFAULT TRUE` | Must-have vs nice-to-have flag |
| `weight` | `NUMERIC(3, 2)` | `NOT NULL DEFAULT 1.0 CHECK (weight >= 0.0 AND weight <= 1.0)` | Skill scoring weight |
| `minimum_years` | `INTEGER` | `NOT NULL DEFAULT 0 CHECK (minimum_years >= 0)` | Minimum required years of experience |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Mapping creation timestamp |
| `CONSTRAINT` | `uq_job_skills_job_skill` | `UNIQUE (job_id, skill_id)` | Prevents duplicate skill mappings |

### 3.8. `resume_skills`
Many-to-many relationship mapping extracted candidate skills from resumes to canonical taxonomy.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Unique mapping ID |
| `resume_id` | `BIGINT` | `NOT NULL`, `FK -> resumes(id) ON DELETE CASCADE` | Associated resume |
| `skill_id` | `BIGINT` | `NOT NULL`, `FK -> skills(id) ON DELETE RESTRICT` | Canonical skill reference |
| `years_experience` | `NUMERIC(4, 1)`| Nullable | Estimated skill experience |
| `extraction_confidence` | `NUMERIC(3, 2)`| `NOT NULL DEFAULT 1.0 CHECK (extraction_confidence >= 0.0 AND extraction_confidence <= 1.0)` | NLP entity extraction confidence |
| `context_snippet` | `VARCHAR(300)` | Nullable | Resume excerpt demonstrating skill |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Mapping creation timestamp |
| `CONSTRAINT` | `uq_resume_skills_resume_skill` | `UNIQUE (resume_id, skill_id)` | Prevents duplicate skill associations |

### 3.9. `blind_screening_results`
Redacted resume views for mitigation of human cognitive and demographic bias during manual review.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Unique blind result ID |
| `resume_id` | `BIGINT` | `NOT NULL UNIQUE`, `FK -> resumes(id) ON DELETE CASCADE` | 1-to-1 association with resume |
| `redacted_text` | `TEXT` | `NOT NULL` | Sanitized plaintext with PII masked |
| `redactions_count` | `INTEGER` | `NOT NULL DEFAULT 0 CHECK (redactions_count >= 0)` | Count of PII entities masked |
| `redactions_log` | `JSONB` | Nullable | Auditable log of entity types and offsets |
| `anonymization_ruleset_version` | `VARCHAR(50)` | `NOT NULL DEFAULT 'v1.0'` | Redaction engine version tag |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Anonymization timestamp |

### 3.10. `bias_reports`
Linguistic diagnostic reports quantifying gender lean, age bias, and exclusionary language in job postings.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Unique report ID |
| `job_id` | `BIGINT` | `NOT NULL`, `FK -> jobs(id) ON DELETE CASCADE` | Associated job description |
| `target_version` | `VARCHAR(20)` | `NOT NULL DEFAULT 'ORIGINAL'`, `CHECK (target_version IN ('ORIGINAL', 'REWRITTEN'))` | Evaluated JD version |
| `gender_bias_score` | `NUMERIC(3, 2)` | `NOT NULL CHECK (gender_bias_score >= 0.0 AND gender_bias_score <= 1.0)` | Normalized gender bias metric |
| `gender_lean` | `VARCHAR(30)` | `NOT NULL`, `CHECK (gender_lean IN ('BALANCED', 'MASCULINE_SKEWED', 'FEMININE_SKEWED'))` | Categorical classification |
| `age_bias_score` | `NUMERIC(3, 2)` | `NOT NULL CHECK (age_bias_score >= 0.0 AND age_bias_score <= 1.0)` | Normalized age bias metric |
| `reading_level` | `VARCHAR(30)` | `NOT NULL`, `CHECK (reading_level IN ('EASY', 'MODERATE', 'DIFFICULT'))` | Readability index category |
| `masculine_terms_count` | `INTEGER` | `NOT NULL DEFAULT 0 CHECK (masculine_terms_count >= 0)` | Flagged masculine coded words |
| `feminine_terms_count` | `INTEGER` | `NOT NULL DEFAULT 0 CHECK (feminine_terms_count >= 0)` | Flagged feminine coded words |
| `age_terms_count` | `INTEGER` | `NOT NULL DEFAULT 0 CHECK (age_terms_count >= 0)` | Flagged age-exclusionary terms |
| `total_flagged_terms` | `INTEGER` | `NOT NULL DEFAULT 0 CHECK (total_flagged_terms >= 0)` | Aggregate count of flagged tokens |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Analysis execution timestamp |

### 3.11. `rewrite_suggestions`
Specific phrase-level replacements proposed to neutralize exclusionary language in job descriptions.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Unique suggestion ID |
| `bias_report_id` | `BIGINT` | `NOT NULL`, `FK -> bias_reports(id) ON DELETE CASCADE` | Associated diagnostic report |
| `original_phrase` | `VARCHAR(100)` | `NOT NULL` | Problematic text segment |
| `suggested_replacement` | `VARCHAR(100)` | `NOT NULL` | Inclusive substitute phrase |
| `category` | `VARCHAR(50)` | `NOT NULL`, `CHECK (category IN ('GENDER_MASCULINE', 'GENDER_FEMININE', 'AGE_EXCLUSIONARY', 'COMPLEX_READABILITY'))` | Bias category |
| `context_snippet` | `VARCHAR(300)` | `NOT NULL` | Surrounding textual excerpt |
| `explanation` | `TEXT` | Nullable | Rationale for the suggestion |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Generation timestamp |

### 3.12. `match_results`
Operational candidate ranking and suitability evaluations for specific job postings.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Unique match result ID |
| `job_id` | `BIGINT` | `NOT NULL`, `FK -> jobs(id) ON DELETE CASCADE` | Evaluated job |
| `candidate_id` | `BIGINT` | `NOT NULL`, `FK -> candidates(id) ON DELETE CASCADE` | Candidate profile |
| `resume_id` | `BIGINT` | `NOT NULL`, `FK -> resumes(id) ON DELETE CASCADE` | Specific evaluated resume snapshot |
| `screening_mode` | `VARCHAR(20)` | `NOT NULL`, `CHECK (screening_mode IN ('NORMAL', 'BLIND'))` | Operational screening configuration |
| `matching_method` | `VARCHAR(30)` | `NOT NULL`, `CHECK (matching_method IN ('KEYWORD', 'SEMANTIC', 'HYBRID'))` | Scoring algorithm |
| `keyword_score` | `NUMERIC(4, 3)` | `NOT NULL CHECK (keyword_score >= 0.0 AND keyword_score <= 1.0)` | TF-IDF / BM25 lexical similarity |
| `semantic_score` | `NUMERIC(4, 3)` | `NOT NULL CHECK (semantic_score >= 0.0 AND semantic_score <= 1.0)` | SBERT dense embedding similarity |
| `skill_jaccard_score` | `NUMERIC(4, 3)`| `NOT NULL CHECK (skill_jaccard_score >= 0.0 AND skill_jaccard_score <= 1.0)` | Normalized skill overlap ratio |
| `final_composite_score` | `NUMERIC(4, 3)`| `NOT NULL CHECK (final_composite_score >= 0.0 AND final_composite_score <= 1.0)` | Weighted composite suitability score |
| `keyword_weight` | `NUMERIC(3, 2)` | `NOT NULL DEFAULT 0.5 CHECK (keyword_weight >= 0.0 AND keyword_weight <= 1.0)` | Lexical component weight factor |
| `semantic_weight` | `NUMERIC(3, 2)` | `NOT NULL DEFAULT 0.5 CHECK (semantic_weight >= 0.0 AND semantic_weight <= 1.0)` | Semantic component weight factor |
| `matched_skills_count` | `INTEGER` | `NOT NULL DEFAULT 0 CHECK (matched_skills_count >= 0)` | Count of intersecting skills |
| `missing_skills_count` | `INTEGER` | `NOT NULL DEFAULT 0 CHECK (missing_skills_count >= 0)` | Count of required skills missing |
| `missing_skills` | `JSONB` | Nullable | List of missing skill names |
| `rank_in_pool` | `INTEGER` | Nullable, `CHECK (rank_in_pool IS NULL OR rank_in_pool >= 1)` | Position in applicant pool |
| `model_version` | `VARCHAR(100)` | `NOT NULL DEFAULT 'all-MiniLM-L6-v2'` | Active embedding model tag |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Evaluation execution timestamp |
| `CONSTRAINT` | `uq_match_job_candidate_mode_method` | `UNIQUE (job_id, candidate_id, screening_mode, matching_method)` | Deterministic scoring uniqueness |

### 3.13. `audit_logs`
Immutable compliance and security audit trails for sensitive screening actions.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Unique audit log entry ID |
| `user_id` | `BIGINT` | Nullable, `FK -> users(id) ON DELETE SET NULL` | Performing user account |
| `action` | `VARCHAR(100)` | `NOT NULL` | Audit action identifier |
| `entity_type` | `VARCHAR(50)` | `NOT NULL` | Target resource domain |
| `entity_id` | `BIGINT` | Nullable | Target resource primary key |
| `details` | `JSONB` | Nullable | Structured action parameters |
| `ip_address` | `VARCHAR(45)` | Nullable | Client IP address (IPv4 / IPv6) |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Action execution timestamp |

#### Privacy & Compliance Governance for Audit Logs:
1. **Purpose of Storing IP Addresses:** The `ip_address` column is maintained strictly for security monitoring, fraud prevention, intrusion detection, and operational access audit trails (e.g., detecting unauthorized unmasking of candidate PII during blind screening sessions).
2. **Exclusion from Research Datasets:** IP addresses are operational metadata and are **strictly prohibited** from inclusion in research datasets, benchmarks, evaluation pipelines, or research telemetry. Research datasets (`dataset_versions`, `dataset_records`) must never incorporate audit logs or client network identifiers.
3. **Research Export Sanitization Policy:** Any export pipeline generating research corpus files, benchmark evaluations, or reproducibility packages must programmatically exclude `audit_logs` and all associated user PII / network metadata.
4. **Data Retention & Access Control:**
   - Operational audit logs should be subject to a strict 90-day to 180-day rolling retention policy with automated archival or truncation.
   - Access to raw audit log records is restricted exclusively to system administrators (`ADMIN` role) and compliance officers; recruiters and candidate users are blocked from direct query access.


---

## 4. Data Dictionary — Research Domain

### 4.1. `dataset_versions`
Versioned, immutable snapshots of benchmarking datasets used for reproducible scientific experiments.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Unique dataset version ID |
| `version_tag` | `VARCHAR(50)` | `NOT NULL UNIQUE` | Semantic version label (e.g., `synth-benchmark-v1.0`) |
| `name` | `VARCHAR(150)` | `NOT NULL` | Dataset title |
| `description` | `TEXT` | `NOT NULL` | Corpus provenance, methodology, and distribution |
| `checksum_sha256` | `VARCHAR(64)` | `NOT NULL` | SHA-256 integrity hash of corpus payload |
| `total_resumes_count` | `INTEGER` | `NOT NULL DEFAULT 0 CHECK (total_resumes_count >= 0)` | Total candidate resumes in corpus |
| `total_jds_count` | `INTEGER` | `NOT NULL DEFAULT 0 CHECK (total_jds_count >= 0)` | Total job descriptions in corpus |
| `lifecycle_status` | `VARCHAR(30)` | `NOT NULL DEFAULT 'DRAFT'`, `CHECK (lifecycle_status IN ('DRAFT', 'VALIDATED', 'FROZEN', 'DEPRECATED'))` | Governance lifecycle status |
| `frozen_at` | `TIMESTAMPTZ` | Nullable | Timestamp when finalized and locked |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Initial ingestion timestamp |

### 4.2. `dataset_records`
Atomic candidate–job benchmark evaluation pairs.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Unique benchmark pair ID |
| `dataset_version_id` | `BIGINT` | `NOT NULL`, `FK -> dataset_versions(id) ON DELETE RESTRICT` | Parent dataset version |
| `record_identifier` | `VARCHAR(100)` | `NOT NULL` | Unique record token within version |
| `jd_title` | `VARCHAR(200)` | `NOT NULL` | Benchmark job title |
| `jd_text` | `TEXT` | `NOT NULL` | Benchmark job description text |
| `jd_required_skills`| `TEXT[]` | `NOT NULL` | Ground truth required skills |
| `candidate_identifier` | `VARCHAR(100)` | `NOT NULL` | Anonymized candidate benchmark ID |
| `candidate_raw_resume` | `TEXT` | `NOT NULL` | Unblinded candidate resume text |
| `candidate_anonymized_resume` | `TEXT` | `NOT NULL` | Pre-blinded candidate resume text |
| `candidate_skills` | `TEXT[]` | `NOT NULL` | Extracted candidate skills |
| `demographic_ground_truth` | `JSONB` | Nullable | Explicitly labeled research demographic metadata. **Never inferred.** |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Record creation timestamp |
| `CONSTRAINT` | `uq_dataset_records_version_identifier` | `UNIQUE (dataset_version_id, record_identifier)` | Prevents duplicate records within a version |

### 4.3. `annotations`
Human expert ground-truth relevance annotations for training and benchmarking evaluation metrics.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Unique annotation ID |
| `dataset_record_id`| `BIGINT` | `NOT NULL`, `FK -> dataset_records(id) ON DELETE CASCADE` | Associated benchmark pair |
| `annotator_id` | `VARCHAR(100)` | `NOT NULL` | Domain expert annotator ID |
| `relevance_score` | `NUMERIC(3, 2)` | `NOT NULL CHECK (relevance_score >= 0.0 AND relevance_score <= 1.0)` | Graded relevance rating |
| `is_binary_match` | `BOOLEAN` | `NOT NULL` | Binary suitability decision |
| `exclusionary_phrases_labeled` | `JSONB` | Nullable | Phrases marked as exclusionary |
| `confidence` | `NUMERIC(3, 2)` | `NOT NULL DEFAULT 1.0 CHECK (confidence >= 0.0 AND confidence <= 1.0)` | Annotator self-reported confidence |
| `status` | `VARCHAR(30)` | `NOT NULL DEFAULT 'SUBMITTED'`, `CHECK (status IN ('DRAFT', 'SUBMITTED', 'VERIFIED'))` | Multi-annotator review status |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Annotation creation timestamp |
| `CONSTRAINT` | `uq_annotations_record_annotator` | `UNIQUE (dataset_record_id, annotator_id)` | Single rating per annotator per record |

### 4.4. `experiment_runs`
Execution log and parameter configuration for controlled research evaluation trials.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Unique experiment run ID |
| `dataset_version_id` | `BIGINT` | `NOT NULL`, `FK -> dataset_versions(id) ON DELETE RESTRICT` | Evaluated benchmark corpus |
| `executor_id` | `BIGINT` | Nullable, `FK -> users(id) ON DELETE SET NULL` | Researcher who triggered run |
| `run_name` | `VARCHAR(150)` | `NOT NULL` | Experiment trial designation |
| `description` | `TEXT` | Nullable | Hypotheses and methodology notes |
| `method` | `VARCHAR(50)` | `NOT NULL`, `CHECK (method IN ('KEYWORD_TFIDF', 'SEMANTIC_SBERT', 'HYBRID', 'BLIND_SEMANTIC'))` | Algorithmic configuration tested |
| `embedding_model` | `VARCHAR(100)` | `NOT NULL DEFAULT 'all-MiniLM-L6-v2'` | Embedding architecture used |
| `model_version` | `VARCHAR(50)` | `NOT NULL DEFAULT '1.0.0'` | Model weight version tag |
| `preprocessing_version` | `VARCHAR(50)` | `NOT NULL DEFAULT 'v1.0-standard'` | Tokenization / cleaning version |
| `anonymization_ruleset_version` | `VARCHAR(50)` | `NOT NULL DEFAULT 'v1.0-ruleset'` | PII masking ruleset version |
| `git_commit_hash` | `VARCHAR(40)` | Nullable | Git commit hash ensuring exact code reproducibility |
| `hyperparameters` | `JSONB` | Nullable | Tuning hyperparameters snapshot |
| `status` | `VARCHAR(30)` | `NOT NULL DEFAULT 'STARTED'`, `CHECK (status IN ('STARTED', 'COMPLETED', 'FAILED'))` | Execution status |
| `duration_ms` | `BIGINT` | Nullable, `CHECK (duration_ms IS NULL OR duration_ms >= 0)` | Total execution runtime |
| `started_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Run start timestamp |
| `completed_at` | `TIMESTAMPTZ` | Nullable | Run finish timestamp |

### 4.5. `experiment_results`
Atomic candidate–JD evaluation scores under experimental runs, tracking rank shifts between normal and blind conditions.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Unique experiment result ID |
| `experiment_run_id` | `BIGINT` | `NOT NULL`, `FK -> experiment_runs(id) ON DELETE CASCADE` | Associated experiment run |
| `dataset_record_id` | `BIGINT` | `NOT NULL`, `FK -> dataset_records(id) ON DELETE RESTRICT` | Evaluated benchmark pair |
| `score_raw` | `NUMERIC(6, 4)` | `NOT NULL` | Raw algorithm match score |
| `score_normalized` | `NUMERIC(4, 3)` | `NOT NULL` | Scaled score in $[0.0, 1.0]$ |
| `normal_rank` | `INTEGER` | Nullable, `CHECK (normal_rank IS NULL OR normal_rank >= 1)` | Candidate rank under standard screening |
| `blind_rank` | `INTEGER` | Nullable, `CHECK (blind_rank IS NULL OR blind_rank >= 1)` | Candidate rank under blind screening |
| `rank_change` | `INTEGER` | `GENERATED ALWAYS AS (normal_rank - blind_rank) STORED` | Exact computed rank shift |
| `execution_latency_ms` | `INTEGER` | Nullable | Inference duration in milliseconds |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Result generation timestamp |
| `CONSTRAINT` | `uq_experiment_results_run_record` | `UNIQUE (experiment_run_id, dataset_record_id)` | Deterministic scoring uniqueness |

### 4.6. `experiment_metrics`
Aggregated statistical evaluation metrics quantifying accuracy, ranking quality, and algorithmic bias.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Unique metric summary ID |
| `experiment_run_id` | `BIGINT` | `NOT NULL UNIQUE`, `FK -> experiment_runs(id) ON DELETE CASCADE` | 1-to-1 association with experiment run |
| `precision_at_k` | `NUMERIC(4, 3)` | `NOT NULL CHECK (precision_at_k >= 0.0 AND precision_at_k <= 1.0)` | Precision@K against ground truth |
| `recall_at_k` | `NUMERIC(4, 3)` | `NOT NULL CHECK (recall_at_k >= 0.0 AND recall_at_k <= 1.0)` | Recall@K against ground truth |
| `f1_score` | `NUMERIC(4, 3)` | `NOT NULL CHECK (f1_score >= 0.0 AND f1_score <= 1.0)` | Harmonic mean of Precision and Recall |
| `ndcg_at_k` | `NUMERIC(4, 3)` | `NOT NULL CHECK (ndcg_at_k >= 0.0 AND ndcg_at_k <= 1.0)` | Normalized Discounted Cumulative Gain |
| `mean_rank_shift` | `NUMERIC(5, 2)` | `NOT NULL DEFAULT 0.0 CHECK (mean_rank_shift >= 0.0)` | Mean absolute rank shift between normal & blind |
| `candidates_with_rank_change_count` | `INTEGER` | `NOT NULL DEFAULT 0 CHECK (candidates_with_rank_change_count >= 0)` | Count of candidates whose rank shifted |
| `jd_bias_score_before` | `NUMERIC(3, 2)` | Nullable, `CHECK (jd_bias_score_before IS NULL OR (jd_bias_score_before >= 0.0 AND jd_bias_score_before <= 1.0))` | Linguistic bias score before rewrite |
| `jd_bias_score_after` | `NUMERIC(3, 2)` | Nullable, `CHECK (jd_bias_score_after IS NULL OR (jd_bias_score_after >= 0.0 AND jd_bias_score_after <= 1.0))` | Linguistic bias score after rewrite |
| `eval_latency_avg_ms` | `NUMERIC(8, 2)` | `NOT NULL DEFAULT 0.0 CHECK (eval_latency_avg_ms >= 0.0)` | Average inference latency per pair |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Evaluation computation timestamp |

---

## 5. Performance Index Strategy

Flyway migration `V1__initial_schema.sql` provisions 19 targeted indexes across foreign key lookup paths, sorting criteria, and composite query constraints:

```sql
-- Application Domain Indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_candidates_email ON candidates(email);
CREATE INDEX idx_resumes_candidate_id ON resumes(candidate_id);
CREATE INDEX idx_resumes_file_hash ON resumes(file_hash_sha256);
CREATE INDEX idx_job_requirements_job_id ON job_requirements(job_id);
CREATE INDEX idx_job_skills_job_id ON job_skills(job_id);
CREATE INDEX idx_resume_skills_resume_id ON resume_skills(resume_id);
CREATE INDEX idx_bias_reports_job_version ON bias_reports(job_id, target_version);
CREATE INDEX idx_rewrite_suggestions_report ON rewrite_suggestions(bias_report_id);
CREATE INDEX idx_match_results_lookup ON match_results(job_id, candidate_id);
CREATE INDEX idx_match_results_scores ON match_results(job_id, final_composite_score DESC);
CREATE INDEX idx_match_results_traceability ON match_results(job_id, candidate_id, resume_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);

-- Research Domain Indexes
CREATE INDEX idx_dataset_records_version ON dataset_records(dataset_version_id);
CREATE INDEX idx_annotations_record ON annotations(dataset_record_id);
CREATE INDEX idx_experiment_runs_dataset ON experiment_runs(dataset_version_id);
CREATE INDEX idx_experiment_results_run ON experiment_results(experiment_run_id);
CREATE INDEX idx_experiment_results_record ON experiment_results(dataset_record_id);
```

---

## 6. Verification and Validation Evidence

All schema objects and Hibernate entity mappings were validated on PostgreSQL 16.13 (`fairhire-postgres`):

- **Flyway Status:** Applied `V1__initial_schema.sql` (Installed Rank 1, Status: `SUCCESS`).
- **Entity Scan:** 19 Spring Data JPA repository interfaces discovered and registered.
- **Hibernate Schema Validator:** Executed with `spring.jpa.hibernate.ddl-auto=validate`. All 19 entities matched exact table schemas, data types, and nullability constraints.
- **Automated Tests:** 11/11 tests passing (`PostgresSchemaValidationTest`, `AuthControllerTest`, `HealthControllerTest`, `JobAndBiasControllerTest`, `FairHireApplicationTests`).
- **Container Health:** All 3 core containers (`fairhire-postgres`, `fairhire-backend`, `fairhire-ai-service`) report `healthy` in Docker Compose.
