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

# LLM
LLM_PROVIDER=ollama            # ollama | openai
LLM_FALLBACK_ENABLED=true
OLLAMA_BASE_URL=http://ollama:11434
OLLAMA_CHAT_MODEL=llama3.1:8b
OLLAMA_EMBED_MODEL=nomic-embed-text
OPENAI_API_KEY=               # optional, only if fallback/openai used
OPENAI_CHAT_MODEL=gpt-4o-mini

# Frontend
FRONTEND_ORIGIN=http://localhost:5173
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

## 6. Optional cloud (stretch)

If you later want a public demo URL: deploy the frontend to a static host and the backend + Postgres to Render/Railway free tier. Ollama won't run on free tiers — for cloud, set `LLM_PROVIDER=openai`. Keep this out of v1; note it in `ROADMAP.md`.

---

## 7. Runbook

| Symptom | Check |
|---|---|
| Backend won't start | Postgres healthy? migrations applied? `JWT_SECRET` set? |
| Analysis 503/slow | Ollama up + models pulled? consider OpenAI fallback for demo |
| Vector search empty | chunks embedded? extension created? index built? |
| CORS error in UI | `FRONTEND_ORIGIN` matches the actual origin |
