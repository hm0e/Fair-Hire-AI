-- =========================================================================
-- FairHire AI - Migration V5: Phase 4 Deterministic Matching Engine
-- Database Engine: PostgreSQL 16
-- Scope: Phase 4 Persistence Foundation (Additive DDL Only)
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
