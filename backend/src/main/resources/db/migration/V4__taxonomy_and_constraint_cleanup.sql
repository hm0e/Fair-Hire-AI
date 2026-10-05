-- ============================================================================
-- FairHire AI — Canonical Relational Database Schema Migration
-- Migration: V4__taxonomy_and_constraint_cleanup.sql
-- Database Engine: PostgreSQL 16
-- Scope: Phase 3B Remediation — Constraint Cleanup, Node Alias Removal, Legacy Skill Deactivation
-- ============================================================================

-- 1. Ensure Legacy Auto-Named Parsing Status Check Constraint is Dropped (B-04)
ALTER TABLE resumes DROP CONSTRAINT IF EXISTS resumes_parsing_status_check;

-- 2. Remediate B-03: Remove ambiguous alias 'Node' from 'Node.js' to prevent false positive matching
UPDATE skills 
SET synonyms = ARRAY['NodeJS', 'Node.JS']
WHERE name = 'Node.js';

-- 3. Controlled Legacy Skills Cleanup (Phase 1/2 Prototype Entities)
-- Update Flask to proper FRAMEWORK category and deactivate legacy non-canonical skills
UPDATE skills
SET category = 'FRAMEWORK', is_active = FALSE
WHERE name = 'Flask';

UPDATE skills
SET is_active = FALSE
WHERE name IN ('Machine Learning', 'Pandas', 'NumPy', 'Data Science');
