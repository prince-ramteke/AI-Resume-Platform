# Deployment & CI/CD
## AI Resume Intelligence Platform

> Local-first via Docker Compose; CI via GitHub Actions. One command to run the whole stack.

---

## 1. Environments & profiles

| Profile | Use | Notes |
|---|---|---|
| `dev` | local run without Docker | Postgres/Ollama expected on localhost |
| `docker` | Compose stack | service hostnames = compose service names |
| `test` | automated tests | Testcontainers spins up Postgres/pgvector |

Spring profile via `SPRING_PROFILES_ACTIVE`. Config precedence: env vars > `application-<profile>.yml` > `application.yml`.

---

## 2. Environment variables (`.env.example`)

```env
# Database
DB_URL=jdbc:postgresql://postgres:5432/resumeai
DB_USER=resumeai
DB_PASSWORD=change_me

# Auth
JWT_SECRET=replace_with_a_long_random_256bit_secret
JWT_EXPIRY_MINUTES=60

# Rate limiting (POST /api/analyses only; in-memory Bucket4j, per user)
ANALYSIS_RATE_LIMIT_CAPACITY=5
ANALYSIS_RATE_LIMIT_REFILL_TOKENS=5
ANALYSIS_RATE_LIMIT_REFILL_PERIOD=15m

# LLM
LLM_PROVIDER=ollama            # ollama | openai
LLM_FALLBACK_ENABLED=true
OLLAMA_BASE_URL=http://ollama:11434
OLLAMA_CHAT_MODEL=llama3.1:8b
OLLAMA_EMBED_MODEL=nomic-embed-text
OPENAI_API_KEY=               # optional, only if fallback/openai used
OPENAI_CHAT_MODEL=gpt-4o-mini
LLM_TEMPERATURE=0.0
LLM_SEED=42
LLM_MAX_PROMPT_TOKENS=3500

# Admin bootstrap
ADMIN_EMAIL=admin@resumeai.local
ADMIN_PASSWORD=$2a$10$...      # pre-hashed BCrypt

# Frontend
FRONTEND_ORIGIN=http://localhost:5173

# Observability (Grafana admin login; dev default only)
GRAFANA_ADMIN_PASSWORD=admin
```

Commit `.env.example` only. `.env` is git-ignored.

---

## 3. docker-compose.yml (services)

```yaml
services:
  postgres:
    image: pgvector/pgvector:pg16
    environment:
      POSTGRES_DB: resumeai
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports: ["5432:5432"]
    volumes: ["pgdata:/var/lib/postgresql/data"]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER}"]
      interval: 5s
      retries: 10

  ollama:
    image: ollama/ollama:latest
    ports: ["11434:11434"]
    volumes: ["ollama:/root/.ollama"]
    # entrypoint script pulls models on first boot:
    # ollama pull llama3.1:8b && ollama pull nomic-embed-text

  backend:
    build: ./backend
    environment:
      SPRING_PROFILES_ACTIVE: docker
      DB_URL: ${DB_URL}
      DB_USER: ${DB_USER}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      OLLAMA_BASE_URL: ${OLLAMA_BASE_URL}
      OPENAI_API_KEY: ${OPENAI_API_KEY}
    ports: ["8080:8080"]
    depends_on:
      postgres: { condition: service_healthy }
      ollama:   { condition: service_started }

  frontend:
    build: ./frontend
    environment:
      VITE_API_BASE_URL: http://localhost:8080/api
    ports: ["5173:5173"]
    depends_on: [backend]

volumes:
  pgdata:
  ollama:
```

Run: `docker-compose up --build` → frontend on :5173, API on :8080, Swagger on :8080/swagger-ui.html.

> First boot pulls Ollama models (a few GB) — document this so a reviewer expects the initial wait.

---

## 4. Dockerfiles

- **backend**: multi-stage — build with `maven:3.9-eclipse-temurin-21` (`mvn -q package -DskipTests`), run on `eclipse-temurin:21-jre`. Copy only the jar. Non-root user. `EXPOSE 8080`.
- **frontend**: build with `node:20` (`npm ci && npm run build`), serve static with `nginx:alpine` (or `vite preview` for simplicity).

---

## 5. CI — GitHub Actions

`.github/workflows/ci.yml`:

```yaml
name: CI
on:
  pull_request:
  push: { branches: [main] }
jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '21', cache: maven }
      - run: cd backend && ./mvnw -B verify   # tests + JaCoCo gate (Testcontainers uses Docker on the runner)
  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '20', cache: npm, cache-dependency-path: frontend/package-lock.json }
      - run: cd frontend && npm ci && npm run build && npm run test --if-present
```

Branch protection on `main`: require the `backend` and `frontend` checks to pass before merge. This is the line on your resume: "CI blocks regressions from reaching main."

---

## 6. Cloud / Portfolio Demo Deployment

Public demo path: static-host frontend + PaaS backend + managed PostgreSQL with pgvector + OpenAI for LLM and embeddings. Ollama is not used; `docker-compose.yml` remains local-dev only.

### 6.1 Prerequisites

- Managed PostgreSQL instance with the `pgvector` extension enabled (`CREATE EXTENSION IF NOT EXISTS vector`). Confirm pgvector ≥ 0.5 (HNSW support required). Providers: Neon, Supabase, Render Postgres, Railway Postgres.
- OpenAI API key with access to `gpt-4o-mini` and `text-embedding-3-small`.
- PaaS account for the backend (Render Web Service, Railway, Fly.io).
- Static host for the frontend (Vercel, Netlify, Cloudflare Pages).

### 6.2 Schema — V7 migration (embedding dimension)

The production DB column is `vector(1536)` to match `text-embedding-3-small`. Migration `V7__embedding_dimension_1536.sql` handles this automatically on first backend startup via Flyway. The production DB must be **fresh** (no pre-existing chunks embedded at 768 dims).

### 6.3 Required production environment variables

Set these in the PaaS platform's secret/env UI. Never commit them.

| Variable | Production value |
|---|---|
| `DB_URL` | `jdbc:postgresql://<host>:5432/resumeai` |
| `DB_USER` | platform-assigned |
| `DB_PASSWORD` | platform-assigned (strong, not `change_me`) |
| `JWT_SECRET` | ≥256-bit random — `openssl rand -hex 32` |
| `JWT_EXPIRY_MINUTES` | `60` |
| `REFRESH_TOKEN_EXPIRY_DAYS` | `7` |
| `LLM_PROVIDER` | `openai` |
| `LLM_FALLBACK_ENABLED` | `false` |
| `EMBEDDING_PROVIDER` | `openai` |
| `EMBEDDING_DIMENSIONS` | `1536` |
| `OPENAI_API_KEY` | real key |
| `OPENAI_CHAT_MODEL` | `gpt-4o-mini` |
| `OPENAI_EMBED_MODEL` | `text-embedding-3-small` |
| `OPENAI_BASE_URL` | `https://api.openai.com` |
| `FRONTEND_ORIGIN` | `https://<your-frontend-domain>` |
| `STORAGE_PATH` | `/app/uploads` (or platform persistent-disk path) |
| `ADMIN_EMAIL` | your admin email |
| `ADMIN_PASSWORD` | pre-hashed BCrypt — `htpasswd -bnBC 10 "" yourpassword \| tr -d ':'` |
| `SPRING_PROFILES_ACTIVE` | `docker` |

`GRAFANA_ADMIN_PASSWORD`, `OLLAMA_*`, and Prometheus/Grafana vars are not needed — the observability stack is not deployed in this path.

### 6.4 Frontend — build-time `VITE_API_BASE_URL`

`VITE_API_BASE_URL` is baked into the static bundle at build time by Vite. Pass it as a Docker build argument or as a build-environment variable on the static host:

```bash
# Docker build (manual)
docker build \
  --build-arg VITE_API_BASE_URL=https://<backend-domain>/api \
  -t resume-ai-frontend ./frontend

# Vercel / Netlify / Cloudflare Pages
# Set VITE_API_BASE_URL as an environment variable in the project settings.
# The static host runs npm run build with that variable present.
```

`FRONTEND_ORIGIN` on the backend must match the static host's origin exactly — CORS will reject all browser requests otherwise.

### 6.5 Deployment order

1. Provision managed Postgres; confirm pgvector extension is enabled.
2. Deploy backend to PaaS with all env vars from §6.3. Flyway runs V1–V7 on first startup — V7 widens the column to `vector(1536)` and rebuilds the HNSW index.
3. Confirm `/actuator/health` returns `UP`.
4. Build frontend image/bundle with `VITE_API_BASE_URL=https://<backend-domain>/api`; deploy to static host.
5. Set `FRONTEND_ORIGIN=https://<frontend-domain>` on the backend and redeploy (env-var change only, no rebuild).
6. Smoke test: register → upload resume → submit JD → run analysis → verify score and evidence appear.

---

## 7. Runbook

| Symptom | Check |
|---|---|
| Backend won't start | Postgres healthy? migrations applied? `JWT_SECRET` set? |
| Analysis 503/slow | Ollama up + models pulled? consider OpenAI fallback for demo |
| Vector search empty | chunks embedded? extension created? index built? |
| CORS error in UI | `FRONTEND_ORIGIN` matches the actual origin |
| Grafana panels empty | Prometheus target `backend:9091` UP? has an analysis run yet to emit metrics? |

---

## 8. Observability (Prometheus + Grafana) — v1.1

Metrics pipeline: **Micrometer → `/actuator/prometheus` → Prometheus → Grafana.**

- **Backend management port `9091`** — Actuator (incl. `prometheus`) runs on a **separate port** (`management.server.port: 9091`) that is **NOT published** to the host in Compose. It is reachable only over the internal Docker network, so metrics are never internet-exposed and no auth change to the app (`8080`) is required. The port is fixed to match `monitoring/prometheus.yml`.
- **Prometheus** (`:9090`) scrapes `backend:9091/actuator/prometheus` every 15s. Config: `monitoring/prometheus.yml` (mounted read-only).
- **Grafana** (`:3000`) — anonymous access disabled, admin password via `GRAFANA_ADMIN_PASSWORD`. Datasource + one dashboard are **provisioned from source-controlled files** under `monitoring/grafana/` (no persistent volume — the dashboard is code).
- **Dashboard** (`monitoring/grafana/dashboards/resume-ai.json`): analysis throughput, success rate, cache-hit rate, analysis latency p50/p95, LLM latency p50/p95 by provider, prompt/completion token rate, plus JVM heap.

Application metrics emitted (bounded, low-cardinality labels only):

| Metric | Type | Labels |
|---|---|---|
| `analysis_count_total` | counter | `result` (success\|failure), `cache` (hit\|miss) |
| `analysis_latency_seconds` | timer (histogram) | — |
| `llm_latency_seconds` | timer (histogram) | `provider` (ollama\|openai) |
| `llm_tokens_total` | counter | `provider`, `type` (prompt\|completion) |

Run: `docker-compose up --build` → Grafana on :3000 (login `admin` / `$GRAFANA_ADMIN_PASSWORD`), Prometheus on :9090. Metrics appear after the first analysis runs.
