-- ============================================================================
-- FairHire AI — Canonical Relational Database Schema Migration
-- Migration: V1__initial_schema.sql
-- Database Engine: PostgreSQL 16
-- Scope: Application Domain (Operational ATS) & Research Domain (Reproducible Experiments)
-- ============================================================================

-- Ensure pgcrypto extension is available for UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- SECTION 1: APPLICATION DOMAIN TABLES
-- ============================================================================

-- 1. Users Table (System actors: Recruiters, Candidates, Administrators)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL CHECK (role IN ('RECRUITER', 'CANDIDATE', 'ADMIN')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Jobs Table (Requisitions & Job Descriptions)
CREATE TABLE jobs (
    id BIGSERIAL PRIMARY KEY,
    creator_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    title VARCHAR(200) NOT NULL,
    department VARCHAR(100),
    raw_description TEXT NOT NULL,
    rewritten_description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('DRAFT', 'ACTIVE', 'CLOSED', 'ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Job Requirements Table (High-Level Qualifications & Policy Gates)
CREATE TABLE job_requirements (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    requirement_type VARCHAR(50) NOT NULL CHECK (requirement_type IN ('EXPERIENCE_YEARS', 'EDUCATION_LEVEL', 'CERTIFICATION', 'LANGUAGE', 'GENERAL')),
    requirement_value VARCHAR(255) NOT NULL,
    necessity VARCHAR(20) NOT NULL DEFAULT 'REQUIRED' CHECK (necessity IN ('REQUIRED', 'PREFERRED')),
    weight NUMERIC(4, 3) NOT NULL DEFAULT 1.000 CHECK (weight >= 0.0 AND weight <= 1.0)
);

-- 4. Candidates Table (Applicant Contact Info — Strictly Demographic-Neutral)
CREATE TABLE candidates (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Resumes Table (Parsed Resume Content & Documents)
CREATE TABLE resumes (
    id BIGSERIAL PRIMARY KEY,
    candidate_id BIGINT NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    file_path VARCHAR(500),
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(20) NOT NULL CHECK (file_type IN ('PDF', 'DOCX', 'TXT')),
    file_hash_sha256 VARCHAR(64) NOT NULL,
    raw_text TEXT NOT NULL,
    parsed_years_experience NUMERIC(4, 1) NOT NULL DEFAULT 0.0 CHECK (parsed_years_experience >= 0.0),
    parsed_education TEXT,
    parsing_status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED' CHECK (parsing_status IN ('PENDING', 'COMPLETED', 'FAILED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Skills Vocabulary Catalog (Normalized Technical Taxonomy)
CREATE TABLE skills (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(50) NOT NULL,
    synonyms TEXT[],
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. Job Skills Mapping (Normalized Machine-Actionable Scoring Inputs)
CREATE TABLE job_skills (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    skill_id BIGINT NOT NULL REFERENCES skills(id) ON DELETE RESTRICT,
    is_mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    minimum_years INTEGER NOT NULL DEFAULT 0 CHECK (minimum_years >= 0),
    weight NUMERIC(4, 3) NOT NULL DEFAULT 1.000 CHECK (weight >= 0.0 AND weight <= 1.0),
    CONSTRAINT uq_job_skills_job_skill UNIQUE (job_id, skill_id)
);

-- 8. Resume Skills Mapping (Extracted Candidate Skills)
CREATE TABLE resume_skills (
    id BIGSERIAL PRIMARY KEY,
    resume_id BIGINT NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    skill_id BIGINT NOT NULL REFERENCES skills(id) ON DELETE RESTRICT,
    extraction_confidence NUMERIC(4, 3) NOT NULL DEFAULT 1.000 CHECK (extraction_confidence >= 0.0 AND extraction_confidence <= 1.0),
    context_snippet VARCHAR(300),
    CONSTRAINT uq_resume_skills_resume_skill UNIQUE (resume_id, skill_id)
);

-- 9. Blind Screening Results Table (Sanitized Text & Redaction Log)
CREATE TABLE blind_screening_results (
    id BIGSERIAL PRIMARY KEY,
    resume_id BIGINT NOT NULL UNIQUE REFERENCES resumes(id) ON DELETE CASCADE,
    anonymized_text TEXT NOT NULL,
    redactions_count INTEGER NOT NULL DEFAULT 0 CHECK (redactions_count >= 0),
    redactions_log JSONB NOT NULL DEFAULT '[]'::jsonb,
    anonymization_ruleset_version VARCHAR(50) NOT NULL DEFAULT 'v1.0',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 10. Bias Reports Table (Linguistic Bias & Readability Auditing)
CREATE TABLE bias_reports (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    target_version VARCHAR(20) NOT NULL DEFAULT 'ORIGINAL' CHECK (target_version IN ('ORIGINAL', 'REWRITTEN')),
    gender_bias_score NUMERIC(5, 4) NOT NULL CHECK (gender_bias_score >= 0.0 AND gender_bias_score <= 1.0),
    gender_lean VARCHAR(30) NOT NULL CHECK (gender_lean IN ('BALANCED', 'MASCULINE_SKEWED', 'FEMININE_SKEWED')),
    age_bias_score NUMERIC(5, 4) NOT NULL CHECK (age_bias_score >= 0.0 AND age_bias_score <= 1.0),
    flesch_reading_ease NUMERIC(5, 2) NOT NULL,
    reading_level VARCHAR(30) NOT NULL CHECK (reading_level IN ('EASY', 'MODERATE', 'DIFFICULT')),
    total_flagged_terms INTEGER NOT NULL DEFAULT 0 CHECK (total_flagged_terms >= 0),
    masculine_terms_count INTEGER NOT NULL DEFAULT 0 CHECK (masculine_terms_count >= 0),
    feminine_terms_count INTEGER NOT NULL DEFAULT 0 CHECK (feminine_terms_count >= 0),
    age_terms_count INTEGER NOT NULL DEFAULT 0 CHECK (age_terms_count >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 11. Rewrite Suggestions Table (Token-Level Inclusive Replacements)
CREATE TABLE rewrite_suggestions (
    id BIGSERIAL PRIMARY KEY,
    bias_report_id BIGINT NOT NULL REFERENCES bias_reports(id) ON DELETE CASCADE,
    original_phrase VARCHAR(100) NOT NULL,
    suggested_replacement VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL CHECK (category IN ('GENDER_MASCULINE', 'GENDER_FEMININE', 'AGE_EXCLUSIONARY', 'COMPLEX_READABILITY')),
    context_snippet VARCHAR(300) NOT NULL,
    is_accepted BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 12. Operational Match Results Table (Production Candidate Screening)
CREATE TABLE match_results (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    candidate_id BIGINT NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    resume_id BIGINT NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    screening_mode VARCHAR(20) NOT NULL CHECK (screening_mode IN ('NORMAL', 'BLIND')),
    matching_method VARCHAR(30) NOT NULL CHECK (matching_method IN ('KEYWORD', 'SEMANTIC', 'HYBRID')),
    keyword_score NUMERIC(5, 4) NOT NULL DEFAULT 0.0 CHECK (keyword_score >= 0.0 AND keyword_score <= 1.0),
    skill_jaccard_score NUMERIC(5, 4) NOT NULL DEFAULT 0.0 CHECK (skill_jaccard_score >= 0.0 AND skill_jaccard_score <= 1.0),
    semantic_score NUMERIC(5, 4) NOT NULL DEFAULT 0.0 CHECK (semantic_score >= -1.0 AND semantic_score <= 1.0),
    keyword_weight NUMERIC(4, 3) NOT NULL DEFAULT 0.300 CHECK (keyword_weight >= 0.0 AND keyword_weight <= 1.0),
    semantic_weight NUMERIC(4, 3) NOT NULL DEFAULT 0.700 CHECK (semantic_weight >= 0.0 AND semantic_weight <= 1.0),
    final_composite_score NUMERIC(5, 4) NOT NULL CHECK (final_composite_score >= 0.0 AND final_composite_score <= 1.0),
    rank_in_pool INTEGER NOT NULL CHECK (rank_in_pool >= 1),
    matched_skills_count INTEGER NOT NULL DEFAULT 0 CHECK (matched_skills_count >= 0),
    missing_skills_count INTEGER NOT NULL DEFAULT 0 CHECK (missing_skills_count >= 0),
    missing_skills JSONB DEFAULT '[]'::jsonb,
    model_version VARCHAR(100) NOT NULL DEFAULT 'all-MiniLM-L6-v2',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_match_results_job_resume_mode_method UNIQUE (job_id, resume_id, screening_mode, matching_method)
);

-- 13. Audit Logs Table (Tamper-Proof Audit Trail)
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    details JSONB,
    ip_address VARCHAR(45),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- SECTION 2: RESEARCH DOMAIN TABLES (REPRODUCIBLE BENCHMARK EXPERIMENTS)
-- ============================================================================

-- 14. Dataset Versions Table (Benchmark Dataset Snapshots)
CREATE TABLE dataset_versions (
    id BIGSERIAL PRIMARY KEY,
    version_tag VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    total_resumes_count INTEGER NOT NULL DEFAULT 0 CHECK (total_resumes_count >= 0),
    total_jds_count INTEGER NOT NULL DEFAULT 0 CHECK (total_jds_count >= 0),
    lifecycle_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' CHECK (lifecycle_status IN ('DRAFT', 'VALIDATED', 'FROZEN', 'DEPRECATED')),
    frozen_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 15. Dataset Records Table (Atomic Candidate-JD Benchmark Pairs)
CREATE TABLE dataset_records (
    id BIGSERIAL PRIMARY KEY,
    dataset_version_id BIGINT NOT NULL REFERENCES dataset_versions(id) ON DELETE RESTRICT,
    record_identifier VARCHAR(100) NOT NULL,
    jd_title VARCHAR(200) NOT NULL,
    jd_text TEXT NOT NULL,
    jd_required_skills TEXT[] NOT NULL,
    candidate_identifier VARCHAR(100) NOT NULL,
    candidate_raw_resume TEXT NOT NULL,
    candidate_anonymized_resume TEXT NOT NULL,
    candidate_skills TEXT[] NOT NULL,
    -- Strictly authorized, explicitly sourced research labels only. Never inferred.
    demographic_ground_truth JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_dataset_records_version_identifier UNIQUE (dataset_version_id, record_identifier)
);

-- 16. Annotations Table (Human Expert Ground Truth)
CREATE TABLE annotations (
    id BIGSERIAL PRIMARY KEY,
    dataset_record_id BIGINT NOT NULL REFERENCES dataset_records(id) ON DELETE CASCADE,
    annotator_id VARCHAR(100) NOT NULL,
    relevance_score INTEGER NOT NULL CHECK (relevance_score BETWEEN 0 AND 3),
    is_match BOOLEAN NOT NULL,
    exclusionary_phrases_labeled JSONB DEFAULT '[]'::jsonb,
    confidence NUMERIC(3, 2) NOT NULL DEFAULT 1.00 CHECK (confidence >= 0.0 AND confidence <= 1.0),
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED' CHECK (status IN ('DRAFT', 'SUBMITTED', 'VERIFIED')),
    is_consensus BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_annotations_record_annotator UNIQUE (dataset_record_id, annotator_id)
);

-- 17. Experiment Runs Table (Deterministic Experiment Executions)
CREATE TABLE experiment_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_name VARCHAR(150) NOT NULL,
    dataset_version_id BIGINT NOT NULL REFERENCES dataset_versions(id) ON DELETE RESTRICT,
    executor_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    method VARCHAR(50) NOT NULL CHECK (method IN ('KEYWORD_TFIDF', 'SEMANTIC_SBERT', 'HYBRID', 'BLIND_SEMANTIC')),
    embedding_model VARCHAR(100) NOT NULL DEFAULT 'all-MiniLM-L6-v2',
    model_version VARCHAR(50) NOT NULL DEFAULT '1.0.0',
    preprocessing_version VARCHAR(50) NOT NULL DEFAULT 'v1.0-standard',
    anonymization_version VARCHAR(50) NOT NULL DEFAULT 'v1.0-ruleset',
    git_commit_hash VARCHAR(40),
    hyperparameters JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(30) NOT NULL DEFAULT 'STARTED' CHECK (status IN ('STARTED', 'COMPLETED', 'FAILED')),
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    duration_ms BIGINT CHECK (duration_ms IS NULL OR duration_ms >= 0),
    error_message TEXT
);

-- 18. Experiment Results Table (Per-Record Candidate Ranking Outputs)
CREATE TABLE experiment_results (
    id BIGSERIAL PRIMARY KEY,
    experiment_run_id UUID NOT NULL REFERENCES experiment_runs(id) ON DELETE CASCADE,
    dataset_record_id BIGINT NOT NULL REFERENCES dataset_records(id) ON DELETE RESTRICT,
    normal_score NUMERIC(6, 5) NOT NULL,
    blind_score NUMERIC(6, 5) NOT NULL,
    normal_rank INTEGER NOT NULL CHECK (normal_rank >= 1),
    blind_rank INTEGER NOT NULL CHECK (blind_rank >= 1),
    rank_change INTEGER GENERATED ALWAYS AS (ABS(normal_rank - blind_rank)) STORED,
    ground_truth_relevance INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_experiment_results_run_record UNIQUE (experiment_run_id, dataset_record_id)
);

-- 19. Experiment Metrics Table (Cohort-Level Evaluative Statistics)
CREATE TABLE experiment_metrics (
    id BIGSERIAL PRIMARY KEY,
    experiment_run_id UUID NOT NULL UNIQUE REFERENCES experiment_runs(id) ON DELETE CASCADE,
    spearman_rho NUMERIC(6, 5) NOT NULL,
    mean_rank_shift NUMERIC(6, 3) NOT NULL CHECK (mean_rank_shift >= 0.0),
    candidates_with_rank_change_count INTEGER NOT NULL CHECK (candidates_with_rank_change_count >= 0),
    precision_at_k NUMERIC(5, 4) NOT NULL CHECK (precision_at_k >= 0.0 AND precision_at_k <= 1.0),
    recall_at_k NUMERIC(5, 4) NOT NULL CHECK (recall_at_k >= 0.0 AND recall_at_k <= 1.0),
    ndcg_at_k NUMERIC(5, 4) NOT NULL CHECK (ndcg_at_k >= 0.0 AND ndcg_at_k <= 1.0),
    f1_score NUMERIC(5, 4) NOT NULL CHECK (f1_score >= 0.0 AND f1_score <= 1.0),
    jd_bias_score_before NUMERIC(5, 4) NOT NULL CHECK (jd_bias_score_before >= 0.0 AND jd_bias_score_before <= 1.0),
    jd_bias_score_after NUMERIC(5, 4) NOT NULL CHECK (jd_bias_score_after >= 0.0 AND jd_bias_score_after <= 1.0),
    bias_reduction_delta NUMERIC(5, 4) NOT NULL,
    eval_latency_avg_ms NUMERIC(8, 2) NOT NULL CHECK (eval_latency_avg_ms >= 0.0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- SECTION 3: PERFORMANCE & INTEGRITY INDEXES
-- ============================================================================

-- Application Domain Indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_candidates_email ON candidates(email);
CREATE INDEX idx_resumes_candidate_id ON resumes(candidate_id);
CREATE INDEX idx_resumes_file_hash ON resumes(file_hash_sha256);
CREATE INDEX idx_job_requirements_job_id ON job_requirements(job_id);
CREATE INDEX idx_job_skills_job_id ON job_skills(job_id);
CREATE INDEX idx_resume_skills_resume_id ON resume_skills(resume_id);
CREATE INDEX idx_match_results_traceability ON match_results(job_id, candidate_id, resume_id);
CREATE INDEX idx_match_results_lookup ON match_results(job_id, screening_mode, matching_method);
CREATE INDEX idx_match_results_scores ON match_results(job_id, rank_in_pool);
CREATE INDEX idx_bias_reports_job_version ON bias_reports(job_id, target_version);
CREATE INDEX idx_rewrite_suggestions_report ON rewrite_suggestions(bias_report_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

-- Research Domain Indexes
CREATE INDEX idx_dataset_records_version ON dataset_records(dataset_version_id);
CREATE INDEX idx_annotations_record ON annotations(dataset_record_id);
CREATE INDEX idx_experiment_runs_dataset ON experiment_runs(dataset_version_id, method);
CREATE INDEX idx_experiment_results_run ON experiment_results(experiment_run_id);
CREATE INDEX idx_experiment_results_record ON experiment_results(dataset_record_id);
