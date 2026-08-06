# System Architecture
## AI Resume Intelligence Platform

> Companion to `PRD.md`. This document is the source of truth for how the system is structured and how data flows through it.

---

## 1. Architectural style

A **modular monolith** backend (single deployable Spring Boot app, package-by-feature) fronted by a **React SPA**, with **Postgres/PGVector** for relational + vector storage and a pluggable **LLM layer** (Ollama local, OpenAI fallback). Everything runs locally via Docker Compose.

Why a modular monolith and not microservices for v1:
- The domain is small; splitting into services would add network, deployment, and consistency overhead with no real benefit.
- Package-by-feature keeps clean boundaries, so it *could* be split later — which is a good thing to say in an interview ("I chose a modular monolith deliberately; here's when I'd split it").

---

## 2. High-level component diagram

```
                        ┌──────────────────────────────┐
                        │        React SPA (Vite)       │
                        │  auth · upload · analyze · UI │
                        └───────────────┬──────────────┘
                                        │ HTTPS/JSON (JWT in header)
                                        ▼
┌───────────────────────────────────────────────────────────────────────┐
│                     Spring Boot Backend (monolith)                      │
│                                                                         │
│  ┌───────────────┐   ┌──────────────────────────────────────────────┐  │
│  │  Security      │   │              Feature modules                 │  │
│  │  filter chain  │   │  auth · resume · jobdescription · analysis   │  │
│  │  (JWT + RBAC)  │   └───────────────┬──────────────────────────────┘  │
│  └───────────────┘                    │                                 │
│                                        ▼                                 │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                          RAG core (rag/)                          │  │
│  │   Ingest → Chunk → Embed → Retrieve → Synthesize → Validate       │  │
│  └───────┬───────────────────────┬───────────────────┬──────────────┘  │
│          │                       │                   │                  │
│          ▼                       ▼                   ▼                  │
│   ┌────────────┐          ┌────────────┐      ┌───────────────┐         │
│   │ Apache Tika│          │ LlmClient  │      │ EmbeddingClient│        │
│   │ (extract)  │          │ abstraction│      │ abstraction    │        │
│   └────────────┘          └─────┬──────┘      └──────┬─────────┘        │
└──────────────────────────────────┼─────────────────┼──────────────────┘
                                    │                 │
                 ┌──────────────────┴───┐      ┌──────┴──────────┐
                 ▼                      ▼      ▼                 ▼
          ┌────────────┐        ┌────────────┐             ┌────────────┐
          │  Ollama    │        │  OpenAI    │             │ Postgres + │
          │  (local)   │        │ (fallback) │             │  PGVector  │
          └────────────┘        └────────────┘             └────────────┘
```

---

## 3. Backend module responsibilities

| Module (package) | Responsibility |
|---|---|
| `config` | Security filter chain, JWT filter, CORS, OpenAPI, bean wiring, `LlmClient`/`EmbeddingClient` selection by profile/config. |
| `auth` | Register, login, JWT issue/validate, user + role persistence. |
| `resume` | Upload, Tika extraction, resume persistence, list/delete. |
| `jobdescription` | Paste/upload JD, persistence, list. |
| `analysis` | Orchestrates an analysis: pulls resume+JD, calls RAG core, persists `Analysis`, returns DTO. The "use case" layer. |
| `rag` | Chunking, embedding orchestration, retrieval (vector/hybrid), prompt assembly, LLM synthesis, output validation. |
| `llm` | `LlmClient` + `OllamaLlmClient`/`OpenAiLlmClient`; `EmbeddingClient` + impls; fallback logic; timeouts. |
| `common` | Shared DTOs, domain exceptions, global exception handler, validation helpers, tracing. |

---

## 4. The RAG analysis pipeline (core flow)

This is the heart of the system. When a user requests an analysis of `resumeId` × `jobDescriptionId`:

```
1. LOAD
   analysis service loads Resume.rawText and JobDescription.rawText (owned by the user)

2. CHUNK
   rag: split each doc into ~500-token chunks, ~50 overlap
   attach metadata: {sourceType: RESUME|JD, sourceId, chunkIndex}

3. EMBED
   EmbeddingClient.embed(chunk) -> float[] for each chunk
   upsert chunks + vectors into DocumentChunk (PGVector)
   (cache: if a doc's chunks already embedded, skip re-embedding)

4. EXTRACT JD REQUIREMENTS
   LLM (or rule pass) turns JD chunks into a list of discrete requirements/skills

5. RETRIEVE EVIDENCE
   for each requirement: vector-search the RESUME chunks (top-k) for supporting evidence
   (v1.1: hybrid = vector + keyword, then re-rank)

6. SYNTHESIZE
   assemble prompt: system instructions + JD requirements + retrieved resume evidence
   LlmClient.complete(prompt) -> JSON verdict
   {score, matchedSkills[], missingSkills[], weakSkills[], recommendations[], evidence[]}

7. VALIDATE & GROUND
   parse JSON -> typed object; validate against schema
   drop any claim whose evidence ref doesn't resolve to a real retrieved chunk
   on malformed output: one repair retry with a stricter instruction, else safe error

8. PERSIST & RETURN
   save Analysis (with provider + latencyMs); map to AnalysisResponse DTO
```

**Design principles baked in here:**
- **Grounding over generation** — the model synthesizes over retrieved evidence; it isn't asked to "remember" the resume.
- **Structured output** — a fixed JSON contract, validated, never free text.
- **Idempotent embedding** — embeddings cached per document to keep p95 latency down.

---

## 5. LLM & embedding abstraction

```java
public interface LlmClient {
    LlmResponse complete(LlmRequest request);   // returns text/JSON + usage
    String providerName();
}

public interface EmbeddingClient {
    float[] embed(String text);
    List<float[]> embedBatch(List<String> texts);
    int dimensions();
}
```

- `OllamaLlmClient` is the default. `OpenAiLlmClient` is the fallback.
- A `ResilientLlmClient` decorator wraps the primary and, on timeout/error (and if `LLM_FALLBACK_ENABLED=true`), retries via the fallback.
- Provider chosen by config: `LLM_PROVIDER=ollama|openai`. Tests inject a `FakeLlmClient` returning canned JSON — no network in unit tests.
- **Why this matters (interview point):** the rest of the app is provider-agnostic. Swapping models is a config change, not a code change. This is the single most important design decision in the project.

---

## 6. Request flows

### 6.1 Auth (login)
```
Client → POST /api/auth/login {email, password}
  → AuthController → AuthService
     → load user, BCrypt.verify
     → issue JWT (roles as claims)
  ← 200 {accessToken, expiresAt}   (401 on bad creds)
```

### 6.2 Resume upload
```
Client → POST /api/resumes  (multipart, JWT)
  → Security filter validates JWT
  → ResumeController (@Valid, file-type/size check)
     → ResumeService → Tika.extract(text)
     → persist Resume(owner=currentUser)
  ← 201 {id, filename, createdAt}
```

### 6.3 Analysis (core)
```
Client → POST /api/analyses {resumeId, jobDescriptionId}  (JWT)
  → AnalysisController
     → AnalysisService (ownership check)
        → RagService.analyze(resume, jd)   // section 4 pipeline
     → persist Analysis
  ← 200 AnalysisResponse {score, matched[], missing[], weak[], recommendations[], evidence[], provider}
```

Full request/response schemas live in `API.md`.

---

## 7. Data flow & storage

- **Relational data** (users, resumes, JDs, analyses) → standard JPA tables in Postgres.
- **Vectors** (`DocumentChunk.embedding`) → PGVector column with an IVFFlat/HNSW index for approximate nearest-neighbor search.
- **JSON verdict fields** (matched/missing/weak/recommendations/evidence) → stored as `JSONB` on `Analysis` for flexibility without extra tables.
- No object storage in v1; raw document text is kept in the row. (Noted as a future change if files get large.)

See `DATABASE.md` for exact DDL and the PGVector extension setup.

---

## 8. Cross-cutting concerns

| Concern | Approach |
|---|---|
| **AuthN/Z** | Spring Security 6 filter chain; stateless JWT; method-level `@PreAuthorize` for RBAC. |
| **Validation** | Bean Validation on all request DTOs; fail fast with 400. |
| **Error handling** | `@RestControllerAdvice` global handler → consistent error envelope `{timestamp, status, error, message, traceId}`. |
| **Observability** | Micrometer metrics (LLM latency, token usage, analysis count); `/actuator/health`; trace id per request in logs. |
| **Config** | Externalized via env vars + Spring profiles (`dev`, `docker`, `test`). Secrets never in source. |
| **Security of prompts** | Uploaded text delimited and labeled as untrusted data in the prompt; system instructions isolated. |

---

## 9. Deployment topology (Docker Compose)

```
docker-compose.yml services:
┌─────────────┐   ┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│  frontend   │   │  backend    │   │  postgres   │   │   ollama    │
│  (React)    │──▶│ (SpringBoot)│──▶│ + pgvector  │   │  (LLM+embed)│
│  :5173      │   │  :8080      │   │  :5432      │◀──│  :11434     │
└─────────────┘   └──────┬──────┘   └─────────────┘   └─────────────┘
                         └───────────────────────────────────▲
                                    backend ↔ ollama
```

- One command: `docker-compose up --build`.
- `depends_on` + healthchecks so backend waits for Postgres and Ollama.
- A one-time init pulls the Ollama models (`llama3.1:8b`, `nomic-embed-text`) via an init container or entrypoint script.
- OpenAI (if enabled) is external — only an API key + base URL, no container.

CI/CD, environments, and profiles are detailed in `DEPLOYMENT.md`.

---

## 10. Key trade-offs (say these in interviews)

| Decision | Chosen | Why | When I'd revisit |
|---|---|---|---|
| Monolith vs. microservices | Modular monolith | Small domain, simpler ops, clean feature boundaries | Split `analysis`/`rag` out if analysis becomes CPU-heavy or independently scaled. |
| Vector store | PGVector in the same Postgres | One datastore, transactional, zero extra infra | Move to a dedicated vector DB (Qdrant/Weaviate) at large scale. |
| LLM provider | Ollama local + OpenAI fallback | Zero-cost dev, quality on demand, provider-agnostic code | Add more providers behind the same interface. |
| Output format | Strict validated JSON | Trust, no hallucinated free text | Add streaming (SSE) for UX in v2. |
| Retrieval | Vector top-k in v1 | Simplest correct baseline | Hybrid + re-rank in v1.1 for precision. |
