# FairHire AI — Database Migration Strategy & Lifecycle Governance

**Version:** 1.0.0 (Phase 2B Complete)  
**Tooling:** Flyway Community Edition, Spring Boot Flyway Auto-Configuration, PostgreSQL 16  

---

## 1. Migration Philosophy & Rules

FairHire AI strictly separates database schema definition from application code. The canonical source of truth for the database schema is the set of versioned SQL migrations executed by Flyway.

### Golden Rules
1. **Never use `ddl-auto=update` or `ddl-auto=create-drop` in production or shared environments:**  
   Hibernate is configured with `spring.jpa.hibernate.ddl-auto=validate`. Hibernate verifies that Java entities match the live database schema at application boot; it never alters tables directly.
2. **Deterministic, Forward-Only Migrations:**  
   Once a Flyway migration has been merged or applied in any shared environment (Docker, Staging, Production), it is **immutable**. You must never edit an existing migration script. Any additions, adjustments, or deprecations must be authored in a new subsequent migration (e.g., `V2__...sql`).
3. **Strict Domain Segregation in SQL:**  
   New migrations must maintain the boundary between the Application Domain and Research Domain. Foreign keys must never cross the isolation boundary from operational candidates to research ground truth.
4. **Dialect Neutrality for Dual Profiling:**  
   Migrations are authored in PostgreSQL 16 DDL. To ensure fast, developer-friendly local slice tests (using the default H2 profile) alongside canonical PostgreSQL verification:
   - Use standard ANSI/PostgreSQL types.
   - For arrays and JSON fields, use `@JdbcTypeCode(SqlTypes.ARRAY)` and `@JdbcTypeCode(SqlTypes.JSON)` in JPA entities so Hibernate binds appropriately on both PostgreSQL (`TEXT[]`, `JSONB`) and H2 (`ARRAY`, `JSON`).

---

## 2. Migration File Naming & Organization

Flyway scripts are stored in:
```
backend/src/main/resources/db/migration/
```

### File Naming Convention
```
V<Version>__<Description_with_underscores>.sql
```

Examples:
- `V1__initial_schema.sql` (Phase 2B baseline: 19 tables, constraints, 19 indexes)
- `V2__add_vector_embeddings_table.sql` (Hypothetical future phase)
- `V3__add_custom_skill_categories.sql`

### Version Numbering Scheme
- Major versions (`V1`, `V2`, `V3`, etc.) correspond to architectural phases or substantial schema increments.
- Minor/patch versions (`V1_1__...`, `V1_2__...`) may be used for targeted hotfixes or localized column additions prior to a major release.
- Note the mandatory **two underscores** (`__`) separating the version number from the human-readable description.

---

## 3. Existing Migration History

### `V1__initial_schema.sql`
- **Installed Rank:** 1
- **Status:** `SUCCESS`
- **Description:** Complete Phase 2B initial schema
- **Contents:**
  - `pgcrypto` extension for cryptographic UUID and hash utilities.
  - **13 Application Domain Tables:** `users`, `jobs`, `job_requirements`, `candidates`, `resumes`, `skills`, `job_skills`, `resume_skills`, `blind_screening_results`, `bias_reports`, `rewrite_suggestions`, `match_results`, `audit_logs`.
  - **6 Research Domain Tables:** `dataset_versions`, `dataset_records`, `annotations`, `experiment_runs`, `experiment_results`, `experiment_metrics`.
  - **54 CHECK constraints:** Numerical score ranges $[0.0, 1.0]$, non-negative counts, positive rank positions, status/category enum sets.
  - **22 Foreign Key constraints:** Explicit cascade policies (`ON DELETE CASCADE` for parent-child dependencies; `ON DELETE RESTRICT` for taxonomies and benchmark versions; `ON DELETE SET NULL` for non-critical creator references).
  - **19 Performance Indexes:** Cover high-traffic query lookups, composite keys, and metric aggregations.

---

## 4. Development Workflow & Reset Procedures

### 4.1. Verifying Schema Locally Against PostgreSQL 16
To execute full schema validation against the canonical PostgreSQL 16 Docker container:

```bash
# 1. Ensure canonical Docker PostgreSQL container is up
docker compose up -d postgres

# 2. Run the dedicated schema validation integration test
cd backend
./mvnw test -Dtest=PostgresSchemaValidationTest
```

This test:
1. Connects to `fairhire-postgres` on port 5433 (or 5432).
2. Triggers Flyway migration execution.
3. Initializes the Spring Boot `LocalContainerEntityManagerFactoryBean`.
4. Runs Hibernate's schema validator against every entity class.

### 4.2. Clean Reset of the Development Database
During active development of an unreleased phase, if you need to wipe and re-apply migrations from scratch:

```bash
# Method A: Via Docker exec (recreates public schema)
docker exec fairhire-postgres psql -U postgres -d fairhire_db -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"

# Method B: Clean wipe of Docker volume
docker compose down -v
docker compose up -d postgres
```

Once the schema is clean, restarting the backend or running `PostgresSchemaValidationTest` will automatically re-apply `V1__initial_schema.sql` and record a clean `flyway_schema_history` row.

---

## 5. Schema Change Checklist for Future Phases (Phase 3+)

Before committing any future database changes:

1. [ ] **Domain Separation Check:** Does the proposed table belong to Application or Research? Verify no foreign keys cross from operational candidate tables to research benchmarks.
2. [ ] **Demographic Privacy Check:** Ensure no protected attributes (gender, ethnicity, age, religion) are added to operational candidate tables or inferred by automated algorithms.
3. [ ] **Data Types:** Use `VARCHAR(n)` or `TEXT` instead of `CHAR(n)` (to avoid `bpchar` padding conflicts). Use `NUMERIC(p, s)` for scores and weights.
4. [ ] **Foreign Key Action:** Explicitly declare `ON DELETE CASCADE` (for child entities) or `ON DELETE RESTRICT` (for referenced lookup/dataset assets).
5. [ ] **Check Constraints:** Add explicit `CHECK` bounds for all scores, counts, and enumerated string states.
6. [ ] **JPA Entity Synchronization:**
   - Create/update entity in `backend/src/main/java/com/fairhire/models/`.
   - Use `@JdbcTypeCode(SqlTypes.ARRAY)` for string arrays.
   - Use `@JdbcTypeCode(SqlTypes.JSON)` for JSONB fields.
   - Set `@Column(insertable = false, updatable = false)` on generated columns.
7. [ ] **Validation Verification:** Execute `./mvnw test -Dtest=PostgresSchemaValidationTest` with `spring.jpa.hibernate.ddl-auto=validate` to verify zero mismatches.
8. [ ] **Docker Compose Verification:** Run `docker compose up -d --build backend` and confirm the health check endpoint returns status `UP` with PostgreSQL 16.13 detected.
