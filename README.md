# FairHire AI — AI-Powered Fair Recruitment & Resume Screening Framework

> **M.Sc. II (Computer Science) — Research Project & Full-Stack Platform**  
> **Authors**: Harsh More (SP47250002) & Saloni Paygude (SP47250009)  
> **Guide**: Prof. Kirti Garud

FairHire AI is an enterprise-grade recruitment and resume screening framework engineered to eliminate algorithmic and cognitive bias from candidate evaluation. It provides reproducible comparisons between traditional keyword ATS scoring, Sentence-BERT semantic matching, and identity-sanitized blind screening.

---

## 🏛 System Architecture

FairHire AI implements a decoupled three-tier microservice architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                 React Frontend (Port 5173)                  │
│   • Recruiter Portal   • Candidate Portal   • Admin Portal  │
└──────────────────────────────┬──────────────────────────────┘
                               │ REST /api (JWT Auth)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│             Spring Boot Backend (Port 8088)                 │
│  • Spring Security + JWT  • JPA Entities & Repositories     │
│  • Orchestration (Jobs, Resumes, Matches, Experiments)      │
│  • PostgreSQL Database Connectivity                         │
└───────────────┬──────────────────────────────┬──────────────┘
                │ JDBC                         │ REST JSON
                ▼                              ▼
┌──────────────────────────────┐ ┌────────────────────────────┐
│      PostgreSQL (Port 5432)  │ │ Python AI Service (:5000)  │
│  • Primary Relational DB     │ │ • FastAPI + PyTorch/S-BERT │
│  • Persistent Audit & Schema │ │ • Parsing, Skills, Bias    │
└──────────────────────────────┘ └────────────────────────────┘
```

---

## 📂 Repository Structure

```
fairhire-ai/
├── frontend/             # React 19 + Vite + Tailwind CSS presentation layer
├── backend/              # Spring Boot 3 core orchestration backend (Java 17/25)
├── ai-service/           # Python FastAPI NLP & embedding microservice
├── dataset/              # Versioned raw, processed, and annotated research datasets
├── experiments/          # Reproducible experiment configurations and logs
├── docs/                 # Architecture, API, and research documentation
├── docker/               # Container build definitions and Nginx configurations
├── docker-compose.yml    # Complete local multi-container composition
├── .env.example          # Environment variables template
└── README.md
```

---

## 🚀 Canonical Setup & Execution Workflow

FairHire AI uses **Docker PostgreSQL 16** as its canonical development database to ensure environment consistency across all machines and platforms.

### Canonical Database Configuration
- **Engine**: PostgreSQL 16 (Alpine Container)
- **Database**: `fairhire_db`
- **Username**: `postgres`
- **Password**: `postgres`
- **Port**: `5432`

---

### Option A: Complete Multi-Container Stack (Recommended)
Run the entire architecture (Frontend, Backend, AI Service, PostgreSQL) in Docker:
```bash
docker compose up --build -d
```
All services start with orchestrated health dependencies:
- Frontend: `http://localhost:5173`
- Backend REST API: `http://localhost:8088` (Composite health: `http://localhost:8088/api/v1/health/system`)
- Python AI Service: `http://localhost:5000` (Health: `http://localhost:5000/api/v1/health`)
- PostgreSQL: `localhost:5432`

---

### Option B: Local Microservices with Canonical Docker DB

#### Step 1: Start Canonical PostgreSQL via Docker
```bash
docker compose up -d postgres
```

#### Step 2: Start Python AI / NLP Service (Port 5000)
```bash
cd ai-service
pip install -r requirements.txt
python -m uvicorn app.main:app --host 0.0.0.0 --port 5000 --reload
```
*Health Check: `GET http://localhost:5000/api/v1/health`*

#### Step 3: Start Spring Boot Backend (Port 8088)
```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```
*Composite Health Check: `GET http://localhost:8088/api/v1/health/system`*

#### Step 4: Start React Frontend (Port 5173)
```bash
cd frontend
npm install
npm run dev
```
*Access in browser at `http://localhost:5173`.*

---

## 🧪 Running Automated Test Suites

```bash
# 1. Spring Boot Backend Tests
cd backend && ./mvnw test

# 2. Python AI Service Tests
cd ai-service && pytest tests/ -v

# 3. Frontend Tests & Build
cd frontend && npm run test && npm run build
```
