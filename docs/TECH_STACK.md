# Tech Stack (with rationale)
## AI Resume Intelligence Platform

> Every choice with the *why* — so you can defend it in an interview. Don't swap without updating this file.

---

## 1. Language & runtime
- **Java 21** — LTS; records, pattern matching, virtual threads available for I/O-bound LLM calls. Matches your resume.
- **Maven** — ubiquitous, simple CI caching, easy multi-stage Docker builds.

## 2. Backend framework
- **Spring Boot 3.x / Spring Framework 6** — industry standard for Java backends; auto-config, actuator, testing support.
- **Spring Web (MVC)** — REST controllers. (WebFlux only if we add SSE streaming in v2.)
- **Spring Data JPA + Hibernate** — repository abstraction, less boilerplate, still lets us drop to native queries for vector search.
- **Spring Security 6** — mature auth/authz; stateless JWT + method security.

## 3. AI / RAG
- **Spring AI** — Spring-native abstraction over chat + embedding models and vector stores; keeps the app provider-agnostic.
- **LangChain4j** (optional/complementary) — richer chains/tools if needed; your resume already lists it.
- **Ollama** — run open models locally (llama3.1:8b for chat, nomic-embed-text for embeddings). Zero cost, data stays local.
- **OpenAI** (fallback) — quality/latency on demand for demos; behind the same interface.
- **Apache Tika** — robust text extraction from PDF/DOCX.

**Why abstraction matters:** the single strongest talking point — "swapping the model is a config change, not a code change."

## 4. Data
- **PostgreSQL 16** — reliable relational store; one datastore for everything.
- **pgvector** — vector similarity search *inside* Postgres. No extra infra, transactional with the rest of the data. Would move to a dedicated vector DB only at large scale.
- **Flyway** — versioned, repeatable migrations.
- **Redis** (v1.1) — caching + rate limiting.

## 5. API concerns
- **springdoc-openapi** — auto-generated Swagger UI; live, always-accurate API docs.
- **Bean Validation (Hibernate Validator)** — declarative input validation.

## 6. Testing
- **JUnit 5 + Mockito** — unit tests; mock repos and LLM clients.
- **Testcontainers** — real Postgres/pgvector in integration tests; no "works on my machine".
- **JaCoCo** — coverage gate in the build.

## 7. Frontend
- **React + Vite + TypeScript** — fast dev, typed, hireable stack.
- **Axios** — typed API client layer.
- **React Router** — protected routing.
- **Tailwind CSS** (or MUI — pick one) — quick, consistent, clean UI.
- **Vitest + React Testing Library** — frontend tests.

## 8. Ops
- **Docker + Docker Compose** — one-command local stack; containerization is a resume signal.
- **GitHub Actions** — CI on every PR; branch protection.
- **Micrometer** (+ Prometheus/Grafana in v1.1) — metrics/observability.

---

## 9. Dependency quick list (backend `pom.xml`)
```
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-validation
spring-boot-starter-actuator
spring-ai-* (ollama, openai, pgvector store)     // per Spring AI BOM
org.postgresql:postgresql
org.flywaydb:flyway-core
org.apache.tika:tika-core, tika-parsers-standard-package
io.jsonwebtoken:jjwt (or spring-security oauth2 jose)
springdoc-openapi-starter-webmvc-ui
-- test --
spring-boot-starter-test
org.testcontainers:junit-jupiter, postgresql
org.mockito:mockito-core
org.jacoco (plugin)
```

---

## 10. Explicitly rejected (and why)
| Rejected | Reason |
|---|---|
| Microservices for v1 | Small domain; ops overhead unjustified. Modular monolith splits later if needed. |
| Dedicated vector DB (Qdrant/Weaviate) | pgvector is enough at this scale; one datastore is simpler. |
| Fine-tuning a model | Unnecessary; RAG over pre-trained models is the right tool and far cheaper. |
| NoSQL primary store | Relational fits the data; JSONB covers flexible verdict fields. |
