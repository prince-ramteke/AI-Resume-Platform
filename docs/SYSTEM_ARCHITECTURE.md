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

4. RETRIEVE EVIDENCE
   vector-search RESUME chunks (top-k, cosine, k ≤ 8) using the full JD text as the query
   filter: source_type = 'RESUME' (JD text is NOT embedded in v1 — it is used only as the query)
   (v1.1: hybrid = vector + keyword, then re-rank — implemented, opt-in; see "Hybrid retrieval" below)

5. SYNTHESIZE (single-pass LLM call)
   assemble prompt: system instructions + full JD text + retrieved resume evidence chunks
   LlmClient.complete(prompt) -> JSON verdict
   the model extracts JD requirements AND scores the resume in one call
   {score, summary, matchedSkills[], missingSkills[], weakSkills[], recommendations[], evidence[]}
   (v1 uses a single LLM call to stay within the 5 s p95 budget; a two-pass
    extract-then-score pipeline may be explored in v1.1 if quality warrants it)

6. VALIDATE & GROUND
   parse JSON -> typed object; validate against schema
   drop any claim whose evidence ref doesn't resolve to a real retrieved chunk
   on malformed output: one repair retry with a stricter instruction, else safe error

7. PERSIST & RETURN
   save Analysis (with provider + latencyMs); map to AnalysisResponse DTO
```

**Result cache (v1.1):** step 1 also runs a scoped lookup on the `analyses` table for a prior row on the same `(userId, resumeId, jobDescriptionId)` whose `createdAt` is at or after the later of `resume.updatedAt` and `jobDescription.updatedAt`. On a hit, steps 2–7 are skipped and the cached DTO is returned; on any edit of either document, the invariant fails naturally and the pipeline runs again.


**Design principles baked in here:**
- **Grounding over generation** — the model synthesizes over retrieved evidence; it isn't asked to "remember" the resume.
- **Structured output** — a fixed JSON contract, validated, never free text.
- **Idempotent embedding** — embeddings cached per document to keep p95 latency down.
- **Single-pass LLM** — one model call per analysis in v1, keeping total latency under the 5 s p95 target.
- **No JD embedding in v1** — the JD is used as the vector-search query, not stored as chunks. JD embedding may be added in v1.1 if cross-analysis similarity search is needed.
- **Token budget** — prompt assembly enforces a hard token cap (model context window minus reserved output tokens). If retrieved chunks exceed the budget, the lowest-ranked chunks are dropped. The budget constant lives in application config (`LLM_MAX_PROMPT_TOKENS`).

**Hybrid retrieval (v1.1):** step 4 can run in a hybrid mode that blends the vector arm with a PostgreSQL full-text (keyword) arm over the same `document_chunks.content`, scoped to the same `(source_type, source_id)`. Each arm returns a candidate pool (`app.rag.hybrid-candidate-pool-size`, default 20); the two rankings are fused by **Reciprocal Rank Fusion** — for a candidate at 1-based rank `r` in a list, contribution `1/(k + r)` with `k = app.rag.hybrid-rrf-k` (default 60), summed across arms — and the top-k survive. The "re-rank" is this deterministic, rank-based fusion; there is **no** ML/cross-encoder re-ranker. A chunk that only the keyword arm surfaces can therefore still reach the prompt, improving recall for exact-term matches (tools, acronyms) that embeddings blur. It is **off by default** (`app.rag.hybrid-enabled=false`), preserving the vector-only baseline; enabling it requires the `V6` full-text index. Evidence shape (`ref`, `snippet`, `sourceType`, `chunkIndex`) is identical in both modes, so grounding, citation, and the `AnalysisResponse` contract are unchanged — only the candidate ordering and the internal `score` value differ.

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
    String providerName();
}
```

- **`EmbeddingClient` is implemented (M4).** `OllamaEmbeddingClient` (default) and `OpenAiEmbeddingClient` (fallback) are thin `RestClient` adapters selected by `app.embedding.provider` via `@ConditionalOnProperty`. `LlmClient` and its impls arrive in M5 with the synthesis step.
- `OllamaLlmClient` is the default. `OpenAiLlmClient` is the fallback.
- A `ResilientLlmClient` decorator wraps the primary and, on timeout/error (and if `LLM_FALLBACK_ENABLED=true`), retries via the fallback.
- Provider chosen by config: `LLM_PROVIDER=ollama|openai`. Tests inject a `FakeLlmClient` returning canned JSON — no network in unit tests.
- **Deterministic scoring:** set `LLM_TEMPERATURE=0.0` and `LLM_SEED=42` (env vars) so the same input produces repeatable scores across runs. Both values are passed through to the provider via `LlmRequest`.
- **Spring AI boundary:** `LlmClient`/`EmbeddingClient` are the application's own interfaces — they define the contract the rest of the app codes against. Spring AI is an *implementation detail* used inside `OllamaLlmClient`/`OpenAiLlmClient` to talk to providers. Feature packages never import Spring AI types directly.
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
  ← 201 AnalysisResponse {score, summary, matched[], missing[], weak[], recommendations[], evidence[], provider}
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
| Retrieval | Vector top-k in v1; opt-in hybrid (vector + keyword, RRF fusion) in v1.1 | Simplest correct baseline, with a deterministic precision/recall boost available behind a config flag | Add a learned re-ranker if RRF proves insufficient. |
