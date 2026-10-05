-- ============================================================================
-- FairHire AI — Canonical Relational Database Schema Migration
-- Migration: V3__skill_taxonomy_and_extraction.sql
-- Database Engine: PostgreSQL 16
-- Scope: Phase 3B Skill Taxonomy, ResumeSkill Metadata, and Lifecycle States
-- ============================================================================

-- 1. Update Resume Parsing Status Lifecycle Check Constraint
ALTER TABLE resumes DROP CONSTRAINT IF EXISTS ck_resumes_parsing_status;
ALTER TABLE resumes ADD CONSTRAINT ck_resumes_parsing_status 
    CHECK (parsing_status IN (
        'UPLOADED',
        'PARSING',
        'PARSED',
        'SKILL_EXTRACTION',
        'READY',
        'FAILED',
        'SKILL_EXTRACTION_FAILED',
        'PENDING',
        'COMPLETED'
    ));

-- 2. Add is_active column to skills table
ALTER TABLE skills ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- 3. Enhance resume_skills with audit and extraction metadata
ALTER TABLE resume_skills ADD COLUMN IF NOT EXISTS matched_text VARCHAR(100);
ALTER TABLE resume_skills ADD COLUMN IF NOT EXISTS extraction_method VARCHAR(50) NOT NULL DEFAULT 'EXACT_CANONICAL_MATCH';
ALTER TABLE resume_skills ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 4. Seed Controlled Canonical Technical Skill Taxonomy (Phase 3B Baseline)
INSERT INTO skills (name, category, synonyms, is_active) VALUES
    ('Java', 'PROGRAMMING_LANGUAGE', ARRAY['Java SE', 'Java EE', 'Core Java', 'Java programming', 'Java 8', 'Java 11', 'Java 17', 'Java 21'], TRUE),
    ('Python', 'PROGRAMMING_LANGUAGE', ARRAY['Python 3', 'Python programming', 'Python3', 'CPython'], TRUE),
    ('JavaScript', 'PROGRAMMING_LANGUAGE', ARRAY['JS', 'ECMAScript', 'ES6', 'ES2015', 'Vanilla JS'], TRUE),
    ('TypeScript', 'PROGRAMMING_LANGUAGE', ARRAY['TS'], TRUE),
    ('C++', 'PROGRAMMING_LANGUAGE', ARRAY['C/C++', 'C plus plus', 'Cpp'], TRUE),
    ('C#', 'PROGRAMMING_LANGUAGE', ARRAY['C Sharp', 'CSharp', 'C-Sharp'], TRUE),
    ('C', 'PROGRAMMING_LANGUAGE', ARRAY['C programming', 'ANSI C'], TRUE),
    ('Go', 'PROGRAMMING_LANGUAGE', ARRAY['Golang'], TRUE),
    ('Rust', 'PROGRAMMING_LANGUAGE', ARRAY['Rust lang'], TRUE),
    ('SQL', 'PROGRAMMING_LANGUAGE', ARRAY['Structured Query Language', 'ANSI SQL'], TRUE),
    ('.NET', 'FRAMEWORK', ARRAY['.NET Core', '.NET Framework', 'dotnet', 'dot net'], TRUE),
    ('Spring Boot', 'FRAMEWORK', ARRAY['SpringBoot', 'Spring Boot Framework', 'Spring Framework'], TRUE),
    ('React', 'FRAMEWORK', ARRAY['React.js', 'ReactJS', 'React Native'], TRUE),
    ('Node.js', 'FRAMEWORK', ARRAY['NodeJS', 'Node.JS', 'Node'], TRUE),
    ('Angular', 'FRAMEWORK', ARRAY['AngularJS', 'Angular 2+', 'Angular 14'], TRUE),
    ('Vue.js', 'FRAMEWORK', ARRAY['Vue', 'VueJS', 'Vue 3'], TRUE),
    ('Django', 'FRAMEWORK', ARRAY['Django Framework', 'Django REST Framework'], TRUE),
    ('FastAPI', 'FRAMEWORK', ARRAY['Fast API'], TRUE),
    ('Docker', 'CLOUD_DEVOPS', ARRAY['Docker containers', 'Docker Compose', 'Containerization'], TRUE),
    ('Kubernetes', 'CLOUD_DEVOPS', ARRAY['K8s', 'Kube'], TRUE),
    ('AWS', 'CLOUD_DEVOPS', ARRAY['Amazon Web Services', 'Amazon AWS'], TRUE),
    ('Azure', 'CLOUD_DEVOPS', ARRAY['Microsoft Azure'], TRUE),
    ('GCP', 'CLOUD_DEVOPS', ARRAY['Google Cloud Platform', 'Google Cloud'], TRUE),
    ('PostgreSQL', 'DATABASE', ARRAY['Postgres', 'PostgreSQL DB'], TRUE),
    ('MySQL', 'DATABASE', ARRAY['MySQL Server'], TRUE),
    ('MongoDB', 'DATABASE', ARRAY['Mongo', 'MongoDB NoSQL'], TRUE),
    ('Redis', 'DATABASE', ARRAY['Redis cache'], TRUE),
    ('Git', 'TOOL', ARRAY['GitHub', 'GitLab', 'Git version control'], TRUE),
    ('Linux', 'TOOL', ARRAY['Unix', 'Ubuntu', 'Linux OS'], TRUE),
    ('REST API', 'ARCHITECTURE', ARRAY['REST', 'RESTful', 'RESTful APIs', 'REST APIs', 'RESTful Web Services'], TRUE),
    ('GraphQL', 'ARCHITECTURE', ARRAY['GraphQL API'], TRUE)
ON CONFLICT (name) DO UPDATE SET
    category = EXCLUDED.category,
    synonyms = EXCLUDED.synonyms,
    is_active = EXCLUDED.is_active;
