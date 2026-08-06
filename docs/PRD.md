# Product Requirements Document (PRD)
## AI Resume Intelligence Platform

> **Version:** 1.0
> **Owner:** Prince Ramteke
> **Status:** Draft → In Development
> **Last updated:** 2026-08-05

---

## 1. Overview

### 1.1 One-line pitch
An AI platform that scores a candidate's resume against a specific job description, explains the gaps, and tells them exactly what to fix — using Retrieval-Augmented Generation (RAG) over the resume and the JD instead of a naive keyword match.

### 1.2 Problem statement
Job seekers apply to dozens of roles and get silently rejected by Applicant Tracking Systems (ATS) and skim-reading recruiters. They don't know *why* they were rejected or *what* to change. Generic "ATS checkers" online do shallow keyword counting and give vague advice.

This platform does something harder and more useful: it semantically understands both documents, produces a defensible match score, identifies missing and weak skills, and returns concrete, prioritized recommendations — with the evidence (which resume/JD chunks drove each conclusion) so the output is trustworthy, not hallucinated.

### 1.3 Why this project (portfolio intent)
This is a resume showcase project for a Java Backend / Gen-AI role. It must demonstrate, in a way a recruiter can verify in five minutes:

- Production-grade **Spring Boot 3.x** REST API design
- A real **RAG pipeline** (parse → chunk → embed → vector search → LLM synthesis)
- An **LLM provider abstraction** (local Ollama with OpenAI fallback)
- **Security** (JWT auth + role-based access control)
- **Testing discipline** (JUnit 5 + Mockito, meaningful coverage)
- **Containerized, one-command local deploy** (Docker Compose)
- A clean **React** frontend so the whole thing is demoable

### 1.4 What this is NOT (explicit non-goals for v1)
- Not a job board or application-tracking product.
- Not a resume *builder/editor* — it analyzes, it doesn't ghost-write the whole resume.
- Not multi-tenant SaaS with billing. Auth exists, but no payments/orgs.
- Not fine-tuning or training models. We use pre-trained LLMs + embeddings only.
- No mobile app. Responsive web is enough.

---

## 2. Target users & personas

| Persona | Goal | Key need |
|---|---|---|
| **Active job seeker (primary)** | Land more interviews | "Score my resume vs. this JD and tell me what to fix, ranked by impact." |
| **Career switcher** | Understand transferable-skill gaps | "Which of my skills map to this role, and what am I clearly missing?" |
| **Recruiter / reviewer (secondary)** | Quickly rank candidates | "Given this JD and 10 resumes, which match best and why?" (batch — stretch goal) |

Primary persona drives v1. The recruiter batch flow is a stretch goal in the roadmap.

---

## 3. User stories

Written as `As a <persona>, I want <capability>, so that <value>`. Each has acceptance criteria (AC).

### Epic A — Accounts & Auth
- **A1.** As a user, I want to register and log in, so that my resumes and analyses are private to me.
  - AC: Email+password registration; passwords BCrypt-hashed; JWT issued on login; protected endpoints reject missing/invalid tokens with 401.
- **A2.** As a user, I want role-based access, so that admin-only endpoints (e.g., system metrics) are protected.
  - AC: Roles `USER` and `ADMIN`; `@PreAuthorize` guards on admin routes; a USER hitting an admin route gets 403.

### Epic B — Resume ingestion
- **B1.** As a user, I want to upload a resume (PDF/DOCX), so that the system can analyze it.
  - AC: Accepts `.pdf`, `.docx`; max 10 MB; rejects other types with 400 + clear message; text extracted via Apache Tika; extraction failure returns a friendly error, never a stack trace.
- **B2.** As a user, I want my uploaded resume stored and re-usable, so that I can score it against multiple JDs without re-uploading.
  - AC: Resume persisted with owner FK; listed on dashboard; deletable.

### Epic C — Job description input
- **C1.** As a user, I want to paste or upload a job description, so that I can compare my resume against it.
  - AC: Accepts pasted text or file upload; stored per-user; reusable.

### Epic D — Analysis (the core)
- **D1.** As a user, I want a match score (0–100) between my resume and a JD, so that I know how strong a fit I am.
  - AC: Score returned in < ~5s (p95) per analysis; score is deterministic-ish (same inputs → within a small tolerance); response includes a short rationale.
- **D2.** As a user, I want a skill-gap breakdown, so that I know what I'm missing.
  - AC: Returns `matchedSkills`, `missingSkills`, `weaklyEvidencedSkills`; each missing skill cites the JD chunk that requires it.
- **D3.** As a user, I want prioritized recommendations, so that I know what to fix first.
  - AC: 3–7 recommendations, each with an impact tag (High/Med/Low) and a one-line "why".
- **D4.** As a user, I want the analysis grounded in evidence, so that I trust it isn't made up.
  - AC: Every claim references the source chunk(s) from resume or JD (source attribution); output is validated against a JSON schema before returning.

### Epic E — History & UI
- **E1.** As a user, I want to see past analyses, so that I can track improvement over time.
  - AC: Analyses listed with score, JD title, timestamp; openable in detail view.
- **E2.** As a user, I want a clear results screen, so that the score, gaps, and fixes are easy to read.
  - AC: Score gauge, matched/missing skill chips, ranked recommendation list, evidence expandable per item.

---

## 4. Functional requirements (system behavior)

### 4.1 RAG analysis pipeline
1. **Ingest** — Extract raw text from resume + JD (Apache Tika).
2. **Chunk** — Split each document into overlapping chunks (target ~500 tokens, ~50 overlap) with metadata (source=RESUME|JD, docId, chunkIndex).
3. **Embed** — Generate vector embeddings per chunk via the embedding model; store in PGVector.
4. **Retrieve** — For each JD requirement, run semantic (vector) search over resume chunks to find best-matching evidence; use hybrid (vector + keyword) where helpful.
5. **Synthesize** — Prompt the LLM with the JD requirements + retrieved resume evidence, instructing it to output a structured JSON verdict (score, matched, missing, weak, recommendations, evidence refs).
6. **Validate** — Parse and validate the LLM JSON against a schema; repair/retry on malformed output; strip anything unsupported by evidence.
7. **Persist & return** — Save the analysis; return it to the client.

### 4.2 LLM provider abstraction
- A single interface (e.g., `LlmClient`) with two implementations: `OllamaLlmClient` (default/local) and `OpenAiLlmClient` (fallback).
- Selection via config/profile; automatic fallback to OpenAI on Ollama failure/timeout (configurable).
- Same abstraction for the embedding model.

### 4.3 Structured output contract
The analysis endpoint always returns the same JSON shape (see API doc), regardless of provider. LLM free-text is never passed straight through.

---

## 5. Non-functional requirements (NFRs)

| Category | Requirement |
|---|---|
| **Performance** | Analysis p95 < 5s with local Ollama on a dev laptop; embedding of a typical resume < 2s. |
| **Reliability** | Graceful degradation: Ollama down → OpenAI fallback; OpenAI absent → clear error, no crash. |
| **Security** | JWT auth, BCrypt passwords, RBAC, input validation on every endpoint, no secrets in code, file-type/size validation, prompt-injection mitigation on ingested text. |
| **Observability** | Structured logs; request tracing id; `/actuator/health`; per-request LLM latency + token count metrics (Micrometer). |
| **Testability** | Service + controller unit tests (JUnit 5 + Mockito); LLM calls mocked in tests; integration tests with Testcontainers (Postgres/PGVector). Target ≥ 75% line coverage on service layer. |
| **Portability** | `docker-compose up` starts DB (PGVector), Ollama, backend, and frontend. No manual steps. |
| **Maintainability** | Layered architecture (controller → service → repository); DTOs at boundaries; global exception handler; documented in `/docs`. |
| **Cost** | Zero-cost default (Ollama local). OpenAI only when explicitly configured. |

---

## 6. Data model (conceptual)

Detailed schema lives in `DATABASE.md`. Conceptual entities:

- **User** (id, email, passwordHash, role, createdAt)
- **Resume** (id, userId FK, filename, rawText, createdAt)
- **JobDescription** (id, userId FK, title, rawText, createdAt)
- **DocumentChunk** (id, sourceType, sourceId, chunkIndex, content, embedding `vector`, metadata) — PGVector table
- **Analysis** (id, userId FK, resumeId FK, jobDescriptionId FK, score, matchedSkills JSONB, missingSkills JSONB, weakSkills JSONB, recommendations JSONB, evidence JSONB, provider, latencyMs, createdAt)

---

## 7. Success metrics (how we judge v1 "done")

**Product-quality signals**
- Analysis returns valid, schema-conformant JSON 100% of the time (validation/repair guarantees it).
- Match score correlates sensibly on a hand-labeled test set of 10 resume/JD pairs (spot-checked).
- Evidence references resolve to real chunks (no dangling refs).

**Engineering signals (the resume payload)**
- One-command startup works from a clean clone.
- CI (GitHub Actions) runs tests on every PR and blocks on failure.
- Service-layer coverage ≥ 75%.
- Swagger UI documents every endpoint.

---

## 8. Release scope

### v1 (MVP — must-have)
Auth, resume upload+parse, JD input, single-pair analysis (score + gaps + recommendations + evidence), history, React UI, Docker Compose, tests, CI.

### v1.1 (should-have)
Hybrid search + re-ranking; analysis caching; per-user rate limiting; metrics dashboard (Grafana).

### v2 (could-have / stretch)
Recruiter batch mode (1 JD × N resumes, ranked); resume improvement suggestions applied inline; streaming results (SSE); export report as PDF.

---

## 9. Risks & mitigations

| Risk | Mitigation |
|---|---|
| LLM hallucinates skills not in resume | Strict evidence grounding + JSON-schema validation + drop unsupported claims. |
| Ollama slow/heavy on dev machine | Small quantized model (e.g., `llama3.1:8b` / `nomic-embed-text`); OpenAI fallback for demos. |
| PDF parsing edge cases (scanned images) | Tika handles text PDFs; document scanned-PDF/OCR as a known limitation for v1. |
| Prompt injection via uploaded text | Treat document text as untrusted data, delimit it clearly in prompts, never let it override system instructions. |
| Scope creep from full-stack UI | UI kept to core screens (auth, upload, analyze, results, history). |

---

## 10. Open questions
- Final embedding model choice (nomic-embed-text vs. others) — decide during spike.
- Chunking strategy tuning (size/overlap) — validate empirically on the test set.
- Whether to keep resume raw text in DB vs. object storage — v1 keeps in DB for simplicity.
