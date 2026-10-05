# FairHire AI — System Architecture Specification

## Architecture Overview

```
                      +-------------------+
                      |   React Frontend  | (Port 5173)
                      +---------+---------+
                                |
                             REST API
                                |
                                v
                      +-------------------+
                      | Spring Boot Core  | (Port 8088 / 8080)
                      +----+---------+----+
                           |         |
                  JDBC/JPA |         | REST JSON
                           v         v
                +------------+     +-------------------+
                | PostgreSQL |     | Python AI Service | (Port 5000)
                | (Port 5432)|     | (FastAPI + S-BERT)|
                +------------+     +-------------------+
```

## Service Responsibilities

### 1. React Frontend
- Presentation layer (Recruiter Studio, Candidate Portal, Admin Research Dashboard, Architecture Viewer).
- Communicates exclusively with Spring Boot REST API (`/api/v1/*`).
- Does NOT communicate directly with PostgreSQL or AI service.

### 2. Spring Boot Core Backend
- Orchestration layer: authentication (JWT), authorization, entity persistence, validation.
- Integrates with PostgreSQL via Spring Data JPA.
- Coordinates calls to Python AI service via `AIServiceClient`.
- Exposes health checks (`/api/v1/health` and `/api/v1/health/system`).

### 3. Python AI / NLP Service
- Dedicated FastAPI microservice running on port 5000.
- Handles document parsing (PDF/DOCX/TXT), skill extraction, Sentence-BERT semantic embeddings, exclusionary language detection, and inclusive rewrite generation.

### 4. PostgreSQL Database
- Canonical relational store (PostgreSQL 16) for all entities (`User`, `Job`, `Candidate`, `Resume`, `Skill`, `Match`, `BiasReport`, `Experiment`, `AuditLog`, etc.).
- Schema managed via reproducible database migrations (Flyway).

### 5. Docker Compose Multi-Container Orchestration
- `docker-compose.yml` declares and orchestrates the complete 4-tier development and production environment:
  - `postgres:16-alpine`: Canonical relational store with health checks.
  - `fairhire-ai-service`: FastAPI application container with CPU-optimized PyTorch and Uvicorn.
  - `fairhire-backend`: Spring Boot executable container running Eclipse Temurin JRE 17 with health probes.
  - `fairhire-frontend`: Alpine Nginx container serving compiled React assets and reverse-proxying `/api` to the backend.
- Manages health check synchronization, volume persistence (`postgres_data`), and isolated network communication.
