# AI Resume Intelligence Platform

Scores a resume against a job description using a Retrieval-Augmented Generation (RAG) pipeline — returning a match score, skill-gap analysis, and prioritized, **evidence-grounded** recommendations. Built to run fully local (free) with Ollama, with an OpenAI fallback for quality on demand.

> Java 21 · Spring Boot 3.x · Spring AI · PostgreSQL + pgvector · React + TypeScript · Docker Compose

---

## Why this exists
Generic "ATS checkers" do shallow keyword counting. This platform semantically understands both documents, produces a defensible score, and cites the exact resume/JD chunks behind every claim — so the output is trustworthy, not hallucinated.

## Highlights (what it demonstrates)
- Production-style **Spring Boot REST API** (layered, DTOs, global error handling, Swagger).
- A real **RAG pipeline**: parse (Apache Tika) → chunk → embed → vector search (pgvector) → LLM synthesis → schema-validated, evidence-grounded output.
- **Provider-agnostic AI layer** — swap Ollama ↔ OpenAI via config, not code.
- **Spring Security 6**: JWT auth + role-based access control + ownership checks.
- **Tested**: JUnit 5 + Mockito + Testcontainers, coverage-gated in CI.
- **One-command run**: `docker-compose up --build`.

## Quick start
```bash
git clone <repo> && cd ai-resume-platform
cp .env.example .env        # set JWT_SECRET, DB_PASSWORD, (optional) OPENAI_API_KEY
docker-compose up --build   # first boot pulls Ollama models (a few GB)
```
- App: http://localhost:5173 · API: http://localhost:8080 · Swagger: http://localhost:8080/swagger-ui.html

## Architecture
See `docs/SYSTEM_ARCHITECTURE.md` for diagrams and the RAG pipeline. Trade-offs (why a modular monolith, why pgvector, why the provider abstraction) are documented there and in `docs/TECH_STACK.md`.

## Documentation map
| Doc | Contents |
|---|---|
| `docs/PRD.md` | Product spec, user stories, scope |
| `docs/SYSTEM_ARCHITECTURE.md` | Components, RAG pipeline, deployment topology |
| `docs/DATABASE.md` | Schema, pgvector, indexing |
| `docs/API.md` | Endpoints & contracts |
| `docs/SECURITY.md` | Auth, RBAC, AI safety, threat model |
| `docs/TESTING.md` | Test strategy & coverage |
| `docs/DEPLOYMENT.md` | Docker Compose & CI/CD |
| `docs/ROADMAP.md` | Milestones (M0–M5, v1.1, v2) |
| `docs/TECH_STACK.md` | Every choice, with rationale |
| `docs/CODING_STANDARDS.md` | Conventions |

## Working with AI assistants
`CLAUDE.md` is the entry point. `.claude/rules/` holds always-on constraints, `.claude/prompts/` holds reusable task recipes, `.claude/commands/` chains them into workflows (`/new-feature`, `/add-endpoint`, `/ship-milestone`).

## Status
In development — see `docs/ROADMAP.md` for milestone progress.

## License
MIT (or your choice).
