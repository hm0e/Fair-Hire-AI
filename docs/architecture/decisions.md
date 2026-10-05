# Architectural Decision Records (ADRs)

## ADR 001: 3-Tier Decoupled Microservice Architecture
- **Date**: 2026-10-03
- **Status**: Accepted
- **Decision**: Adopt React Frontend, Spring Boot Backend (Core Orchestrator), and Python FastAPI AI/NLP Service with PostgreSQL as primary relational database.
- **Reason**: Separates heavy NLP computation (Sentence-BERT embeddings, PDF extraction, bias rule evaluation) from enterprise business logic, transactional persistence, validation, and security in Spring Boot.
- **Alternatives Considered**:
  1. Monolithic Python Flask app: Harder to enforce enterprise transactional isolation and strict separation of presentation/business/ML layers.
  2. Spring Boot with embedded Python (Jython): Unsupported for modern PyTorch and Sentence-Transformers.
- **Consequences**: Requires inter-service HTTP REST communication and containerized orchestration.

## ADR 002: Python AI Service Framework: FastAPI
- **Date**: 2026-10-03
- **Status**: Accepted
- **Decision**: Use FastAPI with Uvicorn rather than Flask for the dedicated AI/NLP service.
- **Reason**: Native asynchronous request handling, automatic OpenAPI/Swagger documentation generation, strict Pydantic type validation, and high throughput for embedding inference.
- **Alternatives Considered**: Flask, Django REST Framework.
- **Consequences**: Requires FastAPI dependencies and Pydantic schema contracts.

## ADR 003: Port Configuration
- **Date**: 2026-10-03
- **Status**: Accepted
- **Decision**: Configure Spring Boot on port `8088` (default configurable via `SERVER_PORT`), Python AI service on port `5000`, Frontend on port `5173`, and PostgreSQL on port `5432`.
- **Reason**: Prevents port conflicts with existing local services (e.g., Jenkins on port 8080).
- **Alternatives Considered**: Defaulting Spring Boot to 8080.
- **Consequences**: Vite proxy and environment variables point to port 8088.
