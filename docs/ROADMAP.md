# Roadmap
## AI Resume Intelligence Platform

> Milestone sequencing. Each milestone ships something demoable and has a Definition of Done (DoD). Build in order — later milestones assume earlier ones.

---

## M0 — Project scaffold  (½–1 day)
Set up the skeleton before any feature.
- Backend Spring Boot project (Maven, Java 21), package-by-feature structure.
- Frontend Vite + React + TS skeleton.
- `docker-compose.yml` with Postgres/pgvector + Ollama + backend + frontend.
- Flyway wired; `V1__init.sql` creates the extension + core tables.
- `.env.example`, `.gitignore`, README, this `docs/` set, `.claude/` rules & prompts.
- GitHub Actions CI running an empty-but-green test.

**DoD:** `docker-compose up` starts everything; `/actuator/health` is green; CI passes.

---

## M1 — Auth & users  (1–2 days)
- User entity, registration, login, BCrypt, JWT issue/validate filter.
- Security filter chain, RBAC (USER/ADMIN), public-route whitelist.
- Global exception handler + error envelope.
- Tests: register/login happy + bad-credential; protected route rejects anon.

**DoD:** can register, log in, and call a protected `GET /api/auth/me`; security integration tests green.

---

## M2 — Document ingestion  (2–3 days)
- Resume upload (multipart), file-type/size validation, Apache Tika extraction, persistence, list/get/delete.
- Job description create (paste + upload), persistence, list/get/delete.
- Ownership enforcement in services.
- Tests: upload validation, extraction, ownership 403.

**DoD:** authenticated user can upload a resume and add a JD and see them listed.

---

## M3 — RAG core (the centerpiece)  (4–6 days)
- `EmbeddingClient` + `LlmClient` abstractions; Ollama impls; OpenAI fallback; `ResilientLlmClient` decorator.
- Chunking + embedding pipeline; `document_chunks` upsert; embedding cache.
- Vector retrieval (top-k) over resume chunks per JD requirement.
- Prompt assembly, structured JSON verdict, schema validation + repair retry, evidence grounding.
- `POST /api/analyses`, persistence, `AnalysisResponse`.
- Tests with `FakeLlmClient`/`FakeEmbeddingClient`: grounding drops dangling refs; repair path; scoring shape.

**DoD:** given a resume + JD, returns a valid, evidence-grounded analysis in < ~5s (p95) locally.

---

## M4 — Frontend  (3–4 days)

> **Numbering note:** git history and the frontend design spec (`docs/frontend/m6-frontend-design.md`) call the frontend **M6**, delivered as sub-milestones M6.1–M6.7. Read "M4" here as the same body of work under that name.

- Auth pages (register/login), token handling, protected routing.
- Upload resume + add JD screens.
- "Run analysis" flow + results screen: score gauge, matched/missing/weak chips, ranked recommendations, expandable evidence.
- History list + detail view.

**DoD:** a recruiter can click through the entire flow in the browser with no API knowledge.

---

## M5 — Polish, observability, hardening  (2–3 days)
- Micrometer metrics (LLM latency, tokens, analysis count); `/actuator` exposed.
- Swagger/OpenAPI complete for every endpoint.
- JaCoCo coverage gate met (≥75% overall).
- README with screenshots, architecture diagram, and the trade-offs section.
- Prompt-injection + file-type security tests.

**DoD:** clean clone → `docker-compose up` → full demo works; CI green; docs match reality.

---

## v1.1 — Should-have (after v1 is solid)
- Hybrid retrieval (vector + keyword) + re-ranking. ✅ — RRF fusion, opt-in via `app.rag.hybrid-enabled` (default off); keyword arm uses the V6 full-text index.
- Analysis result caching (same resume+JD → cached). ✅
- Per-user rate limiting (Bucket4j). ✅ M2 — `POST /api/analyses` only, capacity 5 / refill 5 per 15 min, in-memory (single backend instance; see `docs/SECURITY.md` §7).
- Refresh tokens. ✅
- Grafana + Prometheus dashboard. ✅ — Micrometer → `/actuator/prometheus` (separate unpublished port 9091) → Prometheus → provisioned Grafana dashboard; metrics: analysis count/latency, LLM latency + token usage.

## v2 — Stretch
- Recruiter batch mode (1 JD × N resumes, ranked).
- Streaming results (SSE) for perceived speed.
- PDF report export.
- Inline resume-improvement suggestions.

---

## Sequencing rules
- Never start a milestone before the previous one's DoD is met.
- Every milestone ends with green CI and updated docs.
- Keep each PR to one feature; small and reviewable.
- The RAG core (M3) and the abstraction layer are the resume payload — spend quality time there.
