# CLAUDE.md

Guidance for Claude Code (and any AI assistant) working in this repository.
**Read this file first. Follow it exactly. When in doubt, prefer the rules in `.claude/rules/` and the specs in `docs/`.**

---

## 1. What this project is

**AI Resume Intelligence Platform** — a full-stack app that scores a resume against a job description using a RAG pipeline, returns a match score, skill-gap analysis, and prioritized recommendations, each grounded in evidence from the source documents.

- Product spec: `docs/PRD.md`
- Architecture: `docs/SYSTEM_ARCHITECTURE.md`
- Data model: `docs/DATABASE.md`
- API contract: `docs/API.md`
- Security model: `docs/SECURITY.md`
- Test strategy: `docs/TESTING.md`

If you are about to make a design decision, check whether one of those documents already answers it. Do not silently contradict them — if a doc is wrong, flag it and update the doc in the same change.

---

## 2. Tech stack (do not swap without approval)

**Backend**
- Java 21, Spring Boot 3.x
- Spring Web (REST), Spring Data JPA + Hibernate, Spring Security 6 (JWT + RBAC)
- PostgreSQL + PGVector extension for vector storage
- Spring AI (LLM + embeddings), with a provider abstraction over **Ollama (default/local)** and **OpenAI (fallback)**
- Apache Tika (document text extraction)
- Bean Validation, springdoc-openapi (Swagger), Micrometer (metrics)
- Build: Maven

**Frontend**
- React (Vite) + TypeScript
- Axios for API calls, React Router, Tailwind CSS for styling

**Infra / tooling**
- Docker + Docker Compose (Postgres/PGVector, Ollama, backend, frontend)
- GitHub Actions (CI: build + test on every PR)
- JUnit 5, Mockito, Testcontainers

---

## 3. Repository layout

```
ai-resume-platform/
├── CLAUDE.md                 # this file
├── docker-compose.yml
├── docs/                     # specs — source of truth
│   ├── PRD.md
│   ├── SYSTEM_ARCHITECTURE.md
│   ├── DATABASE.md
│   ├── API.md
│   ├── SECURITY.md
│   ├── TESTING.md
│   ├── DEPLOYMENT.md
│   ├── ROADMAP.md
│   ├── TECH_STACK.md
│   └── CODING_STANDARDS.md
├── .claude/
│   ├── rules/                # always-on constraints (backend, api, security, ...)
│   ├── prompts/              # reusable task prompts (build-feature, fix-bug, ...)
│   └── commands/             # slash-command style workflows
├── backend/                  # Spring Boot app
│   └── src/main/java/com/princeramteke/resumeai/
│       ├── config/           # security, beans, OpenAPI, CORS
│       ├── auth/             # registration, login, JWT
│       ├── resume/           # upload, parsing, storage
│       ├── jobdescription/
│       ├── analysis/         # the RAG + scoring core
│       ├── rag/              # chunking, embedding, retrieval
│       ├── llm/              # LlmClient abstraction + Ollama/OpenAI impls
│       ├── common/           # DTOs, exceptions, global handler, validation
│       └── ResumeAiApplication.java
│   └── src/test/java/...
└── frontend/                 # React app
    └── src/{pages,components,api,hooks,types}/
```

Package-by-feature, not package-by-layer. Each feature package owns its controller, service, repository, and DTOs.

---

## 4. Architecture rules (hard constraints)

1. **Layered flow:** Controller → Service → Repository. Controllers never touch repositories or contain business logic. Services never return JPA entities to controllers — map to DTOs.
2. **DTOs at every boundary.** Never expose entities in API responses or accept them in requests. Use dedicated request/response records.
3. **LLM access only through `LlmClient`.** No feature calls Ollama/OpenAI directly. Same for embeddings via `EmbeddingClient`.
4. **All LLM output is validated.** Parse into typed objects, validate against schema, repair/retry on failure. Never return raw model text as an API result.
5. **Every endpoint validates input** (`@Valid` + Bean Validation) and is covered by the global exception handler. No stack traces leak to clients.
6. **Security by default.** New endpoints are authenticated unless explicitly whitelisted (register/login/health/swagger). Admin routes use `@PreAuthorize`.
7. **No secrets in code or git.** Config via environment variables / `.env` (git-ignored). Provide `.env.example`.
8. **Treat document text as untrusted.** Delimit it in prompts; it must never be able to override system instructions.

---

## 5. Coding conventions

- Java: follow `docs/CODING_STANDARDS.md`. Constructor injection only (no field `@Autowired`). Prefer records for DTOs. Use `Optional` at repository boundaries, not inside deep logic.
- Naming: `XxxController`, `XxxService`, `XxxRepository`, `XxxRequest`/`XxxResponse`.
- Exceptions: throw domain exceptions (e.g., `ResumeNotFoundException`), map centrally to HTTP status in the global handler.
- Logging: SLF4J, structured, include a request/trace id. No `System.out.println`. Never log secrets, tokens, or full document contents.
- Frontend: functional components + hooks, typed API layer in `src/api/`, no business logic in components.

---

## 6. How to build & run

```bash
# Full stack, one command (preferred)
docker-compose up --build

# Backend only (dev)
cd backend && ./mvnw spring-boot:run

# Backend tests
cd backend && ./mvnw test

# Frontend (dev)
cd frontend && npm install && npm run dev
```

- Backend: http://localhost:8080  ·  Swagger: http://localhost:8080/swagger-ui.html
- Frontend: http://localhost:5173
- Postgres/PGVector: localhost:5432  ·  Ollama: localhost:11434

Required env vars (see `.env.example`): `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `OLLAMA_BASE_URL`, `OPENAI_API_KEY` (optional), `LLM_PROVIDER` (`ollama`|`openai`), `LLM_FALLBACK_ENABLED`.

---

## 7. Definition of Done (apply to every change)

A task is done only when ALL are true:
- Code compiles and `./mvnw test` passes.
- New/changed logic has JUnit 5 + Mockito tests; LLM/DB calls mocked in unit tests.
- Inputs validated; errors go through the global handler.
- No secrets committed; `.env.example` updated if config changed.
- Swagger/OpenAPI reflects any API change.
- Relevant `docs/*.md` updated if behavior/contract changed.
- Meaningful commit message.

Never claim work is complete without running the build/tests. Evidence before assertions.

---

## 8. Working style for the assistant

- **Plan before coding** for any non-trivial feature: state the files you'll touch and the approach, then implement.
- **Small, reviewable changes.** One feature/bugfix per change set.
- **Match existing patterns** — read a sibling feature package before adding a new one.
- **Ask, don't assume**, when a spec is ambiguous — but check `docs/` first; the answer is often there.
- When you finish, summarize what changed and what to verify.

---

## 9. Skill usage policy (auto-routing)

Claude should apply specialized skills **automatically but sparingly**. The full map is `docs/SKILL_ROUTING_MAP.md` — consult it when unsure.

**Before making changes:** read the relevant `docs/*.md` and `.claude/rules/<area>.md` first. Repo docs and rules always outrank any external skill. When they conflict with a skill, follow the repo.

**Auto-consult by area** (only what fits the task — never all of them):

- **Backend** (controllers, services, repositories, DTOs, validation, exceptions) → `rules/backend`, `rules/api`, `rules/security`, `rules/database`, `rules/testing`; skill: `engineering:system-design`.
- **Frontend** (React, components, forms, state, styling) → `rules/frontend`, `rules/api`, `rules/testing`; skills: `frontend-design`, `ui-styling`.
- **Database** (schema, entities, relations, migrations, indexes) → `rules/database`, `rules/backend`; skill: `engineering:system-design`.
- **API** (endpoints, request/response, contracts, Swagger/OpenAPI) → `rules/api`, `rules/security`, `rules/backend`; skill: `engineering:system-design`.
- **Security** (auth, JWT, passwords, roles, secrets, CORS, permissions) → `rules/security`, `rules/api`; verify with `engineering:code-review`.
- **Testing** (unit, integration, edge cases, regression) → `rules/testing`; skills: `engineering:testing-strategy`, `test-driven-development`.
- **Deployment** (Docker, Compose, CI/CD, build, hosting) → `rules/deployment`, `rules/security`; skill: `engineering:deploy-checklist`.
- **Documentation** (README, PRD, architecture, roadmap, changelog) → `rules/documentation`; skill: `engineering:documentation`.

**Cross-cutting:** start any new feature with `superpowers:brainstorming` then `writing-plans`; debug with `engineering:debug` / `systematic-debugging`; plan refactors with `engineering:tech-debt`; before claiming done, run `verification-before-completion` and, for merges, `engineering:code-review`.

**Do NOT** auto-apply: personal job-search skills (resume-*), brand/marketing design, ops rituals, memory/plugin tooling, or the doc-format skills (docx/pdf/pptx/xlsx) — those are manual, on explicit request only (see routing map §5). Never force unrelated skills into a task.

## 10. Reference index

| Need | File |
|---|---|
| Why the product exists, scope, user stories | `docs/PRD.md` |
| Components, request flows, RAG pipeline | `docs/SYSTEM_ARCHITECTURE.md` |
| Tables, columns, PGVector setup | `docs/DATABASE.md` |
| Endpoints, request/response shapes, status codes | `docs/API.md` |
| Auth, JWT, RBAC, threat model | `docs/SECURITY.md` |
| What/how to test, coverage targets | `docs/TESTING.md` |
| Docker, CI/CD, environments | `docs/DEPLOYMENT.md` |
| Milestones & sequencing | `docs/ROADMAP.md` |
| Style, formatting, patterns | `docs/CODING_STANDARDS.md` |
| Always-on constraints by area | `.claude/rules/*.md` |
| Which skill for which task | `docs/SKILL_ROUTING_MAP.md` |
| Reusable task workflows | `.claude/prompts/*.md` |
| One-word workflows | `.claude/commands/*.md` |
