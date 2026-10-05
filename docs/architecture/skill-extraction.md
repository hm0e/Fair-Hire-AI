# FairHire AI — Deterministic Skill Extraction & Normalization Pipeline

**Architecture Document — Phase 3B**  
**Version:** `v1.0-rule-based`  
**Status:** IMPLEMENTED & AUDITED  
**Date:** October 2026  

---

> [!IMPORTANT]
> **Deterministic Baseline Notice:**  
> This pipeline implements an auditable, deterministic, rule-based baseline for skill extraction and normalization. It is **NOT** a semantic skill understanding system, vector embedding model, or LLM-driven classifier. Semantic embeddings and transformer-based matching are decoupled and reserved for subsequent research phases.

---

## 1. Overview & Research Objectives

In the FairHire AI recruitment framework, candidate skill extraction is a critical evaluation layer. To experimentally evaluate bias, comparative candidate ranking, and the relative efficacy of keyword vs. semantic screening, the framework requires a **reproducible, explainable, and deterministic skill extraction baseline**.

### Key Pipeline Principles
1. **Explainability & Traceability:** Every extracted skill links directly to its source span, matched token, canonical resolution, context snippet, and rule-based confidence tier.
2. **Strict Deduplication:** Multiple mentions of a skill (e.g., "Java", "Core Java", "java programming") collapse into a single canonical `ResumeSkill` record retaining the highest-confidence evidence.
3. **Robust False-Positive Control:** Word-boundary and symbol-aware tokenization prevent naive substring collisions (e.g., "Java" inside "JavaScript", "C" inside "C++" or "C#", "React" inside "Reaction").
4. **Lifecycle Auditability:** Skill extraction operates strictly after deterministic text extraction, transitioning the resume through `PARSED` $\to$ `SKILL_EXTRACTION` $\to$ `READY` (or `SKILL_EXTRACTION_FAILED`).

```
+--------------------------------------------------------------------------------------------------+
|                                    SKILL EXTRACTION PIPELINE                                     |
+--------------------------------------------------------------------------------------------------+
|                                                                                                  |
|   Parsed Resume Text (Status: PARSED)                                                            |
|          │                                                                                       |
|          ▼                                                                                       |
|   [Active Taxonomy Loader] ──► Fetch active Skill entities (Name, Category, Synonyms)            |
|          │                                                                                       |
|          ▼                                                                                       |
|   [Rule Compilation & Priority Sorting]                                                          |
|     • Compile regex rules for canonical names and aliases                                        |
|     • Sort rules by token length DESC (Greedy longest-match priority)                            |
|     • Symbol-aware boundaries: (?<![a-zA-Z0-9+#])term(?![a-zA-Z0-9+#])                           |
|          │                                                                                       |
|          ▼                                                                                       |
|   [Greedy Text Scanning & Span Reservation]                                                      |
|     • 1st Pass: Exact case matches                                                               |
|     • 2nd Pass: Case-insensitive matches on unreserved spans                                     |
|     • Overlapping span check prevents sub-token theft (e.g. "JavaScript" blocks "Java")          |
|          │                                                                                       |
|          ▼                                                                                       |
|   [Context Snippet Extraction]                                                                   |
|     • Sentence / clause boundary detection (bounded <= 280 chars)                                |
|     • Whitespace and newline normalization                                                       |
|          │                                                                                       |
|          ▼                                                                                       |
|   [Canonical Resolution & Deduplication]                                                         |
|     • Group candidates by canonical Skill.id                                                     |
|     • Retain candidate with highest confidence (ties broken by earliest position)                |
|          │                                                                                       |
|          ▼                                                                                       |
|   [ResumeSkill Persistence]                                                                      |
|     • Idempotent: Flushes previous mappings for resume_id                                        |
|     • Persists: resume_id, skill_id, matched_text, confidence, context_snippet, extraction_method|
|          │                                                                                       |
|          ▼                                                                                       |
|   [Resume Entity Updated] ──► Status: READY, parsing_error = null                                |
|                                                                                                  |
+--------------------------------------------------------------------------------------------------+
```

---

## 2. Skill Taxonomy Design

FairHire AI utilizes a normalized relational taxonomy stored in the `skills` table. Rather than introducing unvetted thousands of unstructured tags, Phase 3B establishes a **controlled development taxonomy** across 6 core technical domains:

| Category | Canonical Skills Included (Baseline) | Sample Aliases / Synonyms |
| :--- | :--- | :--- |
| **`PROGRAMMING_LANGUAGE`** | `Java`, `Python`, `JavaScript`, `TypeScript`, `C++`, `C#`, `C`, `Go`, `Rust`, `SQL` | `Java SE`, `Core Java`, `JS`, `ECMAScript`, `ES6`, `TS`, `C/C++`, `Cpp`, `CSharp`, `Golang`, `ANSI SQL` |
| **`FRAMEWORK`** | `Spring Boot`, `React`, `Node.js`, `Angular`, `Vue.js`, `Django`, `FastAPI`, `.NET` | `SpringBoot`, `Spring Framework`, `React.js`, `ReactJS`, `NodeJS`, `AngularJS`, `VueJS`, `.NET Core`, `dotnet` |
| **`DATABASE`** | `PostgreSQL`, `MySQL`, `MongoDB`, `Redis` | `Postgres`, `PostgreSQL DB`, `MySQL Server`, `Mongo`, `Redis cache` |
| **`CLOUD_DEVOPS`** | `Docker`, `Kubernetes`, `AWS`, `Azure`, `GCP` | `Docker containers`, `Docker Compose`, `K8s`, `Kube`, `Amazon Web Services`, `Microsoft Azure`, `Google Cloud` |
| **`TOOL`** | `Git`, `Linux` | `GitHub`, `GitLab`, `Git version control`, `Unix`, `Ubuntu` |
| **`ARCHITECTURE`** | `REST API`, `GraphQL` | `REST`, `RESTful`, `RESTful APIs`, `REST APIs`, `GraphQL API` |

### Taxonomy Expansion Protocol
As research benchmark datasets (e.g. `dataset_records.jd_required_skills`) are integrated:
1. New skills must be registered with an explicit category and canonical name.
2. Synonyms must be curated to avoid polysemous colloquialisms.
3. Inactive skills can be flagged with `is_active = FALSE` to deprecate without breaking historical foreign keys in `resume_skills` or `job_skills`.

---

## 3. Extraction Method & False-Positive Controls

### 3.1. Symbol-Aware Boundary Handling
Standard regex word boundaries (`\b`) fail on technical programming terminology:
- In `\bC++\b`, the trailing `+` is a non-word character; `\b` fails immediately.
- In `\b.NET\b`, the leading `.` is a non-word character; `\b` fails immediately.
- In `\bC\b`, without looking ahead for `+` or `#`, it would falsely match the `C` in `C++` or `C#`.

To resolve this deterministically, `DeterministicSkillExtractor` constructs custom boundary assertions:

```java
// Boundary regex examples:
// C++:
"(?<![a-zA-Z0-9+#])C\\+\\+(?![a-zA-Z0-9+#])"

// C#:
"(?<![a-zA-Z0-9+#])C#(?![a-zA-Z0-9+#])"

// Isolated C:
"(?<![a-zA-Z0-9+#/._-])C(?![a-zA-Z0-9+#/._-])"

// .NET:
"(?<![a-zA-Z0-9.])\\.NET(?![a-zA-Z0-9])"

// Node.js:
"(?<![a-zA-Z0-9])Node\\.js(?![a-zA-Z0-9])"

// General terms (e.g. Java, Python, React):
"(?<![a-zA-Z0-9])" + Pattern.quote(term) + "(?![a-zA-Z0-9+#])"
```

### 3.2. Priority Ordering & Greedy Span Reservation
To prevent sub-token collisions (e.g. "JavaScript" matching "Java", "C++" matching "C", "React.js" matching "React"):
1. Rules are sorted by **token character length in descending order**.
2. When a longer term matches (e.g. "JavaScript" at characters `[15, 25]`), that span is recorded in an `occupiedSpans` registry.
3. When shorter rules are subsequently evaluated (e.g. "Java" at `[15, 19]`), the candidate span overlaps with `[15, 25]` and is rejected immediately.
4. Single-letter terms (e.g. `C`) are strictly case-sensitive to prevent matching common English words, initials, or bullet characters.

---

## 4. Heuristic Confidence Model

The extraction pipeline assigns transparent heuristic confidence weights based on the precision of the rule matched:

| Method Enum | Heuristic Weight | Description & Example |
| :--- | :---: | :--- |
| `EXACT_CANONICAL_MATCH` | **`1.000`** | Exact case and boundary match against canonical name (e.g., `"Java"` $\to$ `Java`). |
| `CASE_INSENSITIVE_CANONICAL_MATCH` | **`0.970`** | Case-insensitive match against canonical name (e.g., `"java"` $\to$ `Java`). |
| `EXACT_ALIAS_MATCH` | **`0.950`** | Exact case match against a curated synonym (e.g., `"SpringBoot"` $\to$ `Spring Boot`). |
| `CASE_INSENSITIVE_ALIAS_MATCH` | **`0.920`** | Case-insensitive match against a curated synonym (e.g., `"springboot"` $\to$ `Spring Boot`). |
| `NORMALIZED_MATCH` | **`0.880`** | Match resolved via whitespace or punctuation normalization (e.g., `"NodeJS"` $\to$ `Node.js`). |

> [!NOTE]
> These values are deterministic heuristic weights for rule-based sorting and audit ranking. They are explicitly documented as non-statistical heuristic scores.

---

## 5. Context Snippet Extraction

For auditability, every detected skill extracts surrounding sentence evidence:
- **Backward search:** Scans up to 70 characters before match start for sentence starts (`. `, `\n`).
- **Forward search:** Scans up to 70 characters after match end for sentence ends (`. `, `\n`).
- **Whitespace normalization:** Replaces tabs and newlines with single spaces.
- **Length bound:** Bounded to a maximum of 280 characters (with `...` truncation if necessary) to fit cleanly in the database column (`VARCHAR(300)`).

*Example:*  
- **Matched Text:** `"SpringBoot"`
- **Context Snippet:** `"Backend services built using SpringBoot, FastAPI, and Node.js."`

---

## 6. Duplicate Resolution & Idempotency

When a resume mentions a canonical skill multiple times across the document:
- All candidate mentions are grouped by `Skill.id`.
- The candidate with the **highest confidence score** is selected.
- If confidence scores are tied, the candidate appearing earlier in the document with full context is selected.
- Exactly one `ResumeSkill` row is created, strictly respecting `UNIQUE (resume_id, skill_id)`.
- If re-running skill extraction on an existing resume, `deleteByResumeId` followed by an immediate Hibernate `flush()` ensures existing records are cleared before inserting new rows without constraint violations.

---

## 7. Resume Lifecycle State Transitions

The complete resume ingestion lifecycle now encompasses Phase 3B:

```
[Upload Request Received]
           │
           ▼
     ┌───────────┐
     │ UPLOADED  │  (File validated, SHA-256 computed, binary stored)
     └─────┬─────┘
           │
           ▼
     ┌───────────┐
     │  PARSING  │  (Apache PDFBox / POI extracting text)
     └─────┬─────┘
           │
           ├────────────────────────────► FAILED (Invalid text / Scanned OCR needed)
           ▼
     ┌───────────┐
     │  PARSED   │  (Text normalized, char/word counts computed)
     └─────┬─────┘
           │
           ▼
     ┌──────────────────┐
     │ SKILL_EXTRACTION │  (Scanning text against canonical taxonomy)
     └─────┬────────────┘
           │
           ├────────────────────────────► SKILL_EXTRACTION_FAILED (Error logged, raw text preserved)
           ▼
     ┌───────────┐
     │   READY   │  (Canonical ResumeSkills persisted; ready for matching)
     └───────────┘
```

---

## 8. Database Schema & Flyway V3

Flyway migration `V3__skill_taxonomy_and_extraction.sql` applied the following structural enhancements:

```sql
-- 1. Update Resume Parsing Status Lifecycle Check Constraint
ALTER TABLE resumes DROP CONSTRAINT IF EXISTS resumes_parsing_status_check;
ALTER TABLE resumes DROP CONSTRAINT IF EXISTS ck_resumes_parsing_status;
ALTER TABLE resumes ADD CONSTRAINT ck_resumes_parsing_status 
    CHECK (parsing_status IN (
        'UPLOADED', 'PARSING', 'PARSED', 'SKILL_EXTRACTION', 'READY', 
        'FAILED', 'SKILL_EXTRACTION_FAILED', 'PENDING', 'COMPLETED'
    ));

-- 2. Add is_active column to skills table
ALTER TABLE skills ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- 3. Enhance resume_skills with audit and extraction metadata
ALTER TABLE resume_skills ADD COLUMN IF NOT EXISTS matched_text VARCHAR(100);
ALTER TABLE resume_skills ADD COLUMN IF NOT EXISTS extraction_method VARCHAR(50) NOT NULL DEFAULT 'EXACT_CANONICAL_MATCH';
ALTER TABLE resume_skills ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 4. Seed Canonical Technical Skill Taxonomy
INSERT INTO skills (name, category, synonyms, is_active) VALUES
    ('Java', 'PROGRAMMING_LANGUAGE', ARRAY['Java SE', 'Core Java', 'Java 17', ...], TRUE),
    ...
ON CONFLICT (name) DO UPDATE SET ...;
```

---

## 9. Research Traceability & Versioning

To ensure reproducible benchmark experiments across different points in time, every extraction run operates against documented version identifiers:

- `skill_taxonomy_version`: `v1.0`
- `skill_extractor_version`: `v1.0-rule-based`
- `normalization_version`: `v1.0`

Given identical resume text and taxonomy catalog, the algorithm produces **100% byte-for-byte identical extracted skills, confidence scores, and context snippets** across runs and environments.

---

## 10. Limitations & Future Scope

1. **Rule-Based Coverage:**
   - Novel skill acronyms or uncataloged technologies will not be recognized until added to the taxonomy catalog.
2. **Polysemy:**
   - Single-letter languages (like `C`) are restricted to uppercase standalone tokens. Complex domain-specific polysemy (e.g. "Spring" as season vs framework) relies on curated aliases (e.g. "Spring Boot", "Spring Framework").
3. **No Semantic Inferences:**
   - The extractor does not infer that a "React" developer implicitly knows "JavaScript" unless both are explicitly mentioned in the text. Explicit inference is reserved for downstream evaluation.
