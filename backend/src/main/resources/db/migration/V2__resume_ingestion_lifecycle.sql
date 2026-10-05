-- FairHire AI - Phase 3A Migration: Resume Ingestion Lifecycle & Parser Metadata
-- Version: V2__resume_ingestion_lifecycle.sql

-- 1. Update parsing_status check constraint to support UPLOADED -> PARSING -> PARSED / FAILED lifecycle
ALTER TABLE resumes DROP CONSTRAINT IF EXISTS resumes_parsing_status_check;
ALTER TABLE resumes ADD CONSTRAINT resumes_parsing_status_check 
    CHECK (parsing_status IN ('UPLOADED', 'PARSING', 'PARSED', 'FAILED', 'PENDING', 'COMPLETED'));

ALTER TABLE resumes ALTER COLUMN parsing_status SET DEFAULT 'UPLOADED';

-- 2. Allow raw_text to default to empty string during initial upload stage
ALTER TABLE resumes ALTER COLUMN raw_text SET DEFAULT '';

-- 3. Add parser version tracking, error diagnostics, and file size metadata
ALTER TABLE resumes ADD COLUMN IF NOT EXISTS parser_version VARCHAR(50) NOT NULL DEFAULT 'v1.0-deterministic';
ALTER TABLE resumes ADD COLUMN IF NOT EXISTS parsing_error TEXT;
ALTER TABLE resumes ADD COLUMN IF NOT EXISTS file_size_bytes BIGINT;

-- 4. Add index for parsing status lookups
CREATE INDEX IF NOT EXISTS idx_resumes_parsing_status ON resumes(parsing_status);
