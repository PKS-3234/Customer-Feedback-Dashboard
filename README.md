# Customer Feedback Intelligence Platform

An enterprise-style customer feedback analysis platform that ingests customer feedback data, uses Google Gemini for AI analysis, generates embeddings, performs semantic search using PGVector, supports multi-agent workflows, and uses LangGraph for orchestration.

## Architecture

```
Angular (Port 4200)
    ↓
Spring Boot (Port 8080)
    ↓
FastAPI (Port 8000)
    ↓
Google Gemini API (gemini-2.5-flash & gemini-embedding-001)

PostgreSQL 16 + PGVector (Port 5432)
```

## Tech Stack

- **Frontend**: Angular 21, Standalone Components, Angular Router, Angular HttpClient
- **Backend**: Spring Boot 3.3+, Java 21, Maven
- **AI Service**: FastAPI, Python 3.12, Pydantic v2
- **Database**: PostgreSQL 16 + PGVector extension
- **AI Provider**: Google Gemini API (gemini-2.5-flash, gemini-embedding-001)

## Implementation Roadmap

- [x] **Phase 1: Infrastructure** (Repository structure, Docker PGVector, Spring Boot foundation, FastAPI foundation, Angular 21 foundation)
- [ ] **Phase 2: CSV Ingestion**
- [ ] **Phase 3: Gemini Analysis**
- [ ] **Phase 4: Embeddings & Semantic Search**
- [ ] **Phase 5: Multi-Agent System**
- [ ] **Phase 6: Executive Dashboard**
- [ ] **Phase 7: LangGraph Orchestration**

## Phase 1 Quickstart

### 1. Database (PostgreSQL + PGVector)
```bash
docker compose up -d
```

### 2. AI Service (FastAPI)
```bash
cd ai-service
python -m venv .venv
# Windows:
.venv\Scripts\activate
# Linux/macOS:
# source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### 3. Business Backend (Spring Boot)
```bash
cd backend-spring
mvn clean spring-boot:run
```

### 4. Frontend (Angular 21)
```bash
cd frontend-angular
npm install
npm start
```
