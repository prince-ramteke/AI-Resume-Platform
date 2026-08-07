# Database Design
## AI Resume Intelligence Platform

> Source of truth for schema, PGVector setup, and indexing. Keep in sync with JPA entities.

---

## 1. Engine & extensions

- **PostgreSQL 16** with the **pgvector** extension for embedding storage + ANN search.

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

Embedding dimension depends on the model. `nomic-embed-text` → **768**. If you switch models, update the `vector(N)` dimension and re-embed. Keep the dimension in one config constant so it isn't hardcoded in multiple places.

---

## 2. Entity relationship overview

```
users 1───∞ resumes
users 1───∞ job_descriptions
users 1───∞ analyses
resumes 1───∞ analyses
job_descriptions 1───∞ analyses
document_chunks  ──(sourceType, sourceId)──▶ resumes | job_descriptions   (logical FK)
```

`document_chunks` uses a polymorphic `(source_type, source_id)` reference rather than a hard FK, because a chunk can belong to either a resume or a job description.

---

## 3. Tables (DDL)

### 3.1 users
```sql
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,          -- BCrypt
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',  -- USER | ADMIN
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_users_email ON users (email);
```

### 3.2 resumes
```sql
CREATE TABLE resumes (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    filename     VARCHAR(255) NOT NULL,
    raw_text     TEXT         NOT NULL,
    content_type VARCHAR(100),                -- MIME type (application/pdf, etc.)
    file_size    BIGINT,                      -- bytes
    file_path    VARCHAR(500),                -- path in local/cloud storage
    page_count   INT,                         -- extracted via Tika
    language     VARCHAR(10),                 -- detected document language
    deleted      BOOLEAN      NOT NULL DEFAULT false,  -- soft delete
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ                  -- set on replace/update
);
CREATE INDEX idx_resumes_user ON resumes (user_id);
CREATE INDEX idx_resumes_user_active ON resumes (user_id) WHERE deleted = false;
```

> The `V2__resume_metadata.sql` migration adds `content_type`, `file_size`, `file_path`, `page_count`, `language`, `deleted`, `updated_at`, and the partial index on active resumes.

### 3.3 job_descriptions
```sql
CREATE TABLE job_descriptions (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title        VARCHAR(255) NOT NULL,
    raw_text     TEXT         NOT NULL,
    content_type VARCHAR(100),                -- MIME type (application/pdf, text/plain, etc.)
    file_size    BIGINT,                      -- bytes (null for text-paste JDs)
    file_path    VARCHAR(500),                -- path in local/cloud storage (null for text-paste)
    page_count   INT,                         -- extracted via Tika (null for TXT / text-paste)
    language     VARCHAR(10),                 -- detected document language
    deleted      BOOLEAN      NOT NULL DEFAULT false,  -- soft delete
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ                  -- set on update
);
CREATE INDEX idx_jd_user ON job_descriptions (user_id);
CREATE INDEX idx_jd_user_active ON job_descriptions (user_id) WHERE deleted = false;
CREATE INDEX idx_jd_title ON job_descriptions USING gin (to_tsvector('english', title));
```

> The `V3__job_description_metadata.sql` migration adds `content_type`, `file_size`, `file_path`, `page_count`, `language`, `deleted`, `updated_at`, the partial index on active JDs, and a GIN index on title for full-text search.

### 3.4 document_chunks  (PGVector)
```sql
CREATE TABLE document_chunks (
    id          BIGSERIAL PRIMARY KEY,
    source_type VARCHAR(10) NOT NULL,       -- RESUME | JD
    source_id   BIGINT      NOT NULL,
    chunk_index INT         NOT NULL,
    content     TEXT        NOT NULL,
    embedding   vector(768) NOT NULL,
    metadata    JSONB       NOT NULL DEFAULT '{}',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (source_type, source_id, chunk_index)
);

-- ANN index for cosine similarity (build AFTER bulk load for best results)
CREATE INDEX idx_chunks_embedding
    ON document_chunks
    USING hnsw (embedding vector_cosine_ops);

CREATE INDEX idx_chunks_source ON document_chunks (source_type, source_id);
```

> HNSW gives better recall/latency than IVFFlat for this scale and needs no `lists` tuning. If you prefer IVFFlat: `USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100)` and run `ANALYZE` after load.

### 3.5 analyses
```sql
CREATE TABLE analyses (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    resume_id            BIGINT NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    job_description_id   BIGINT NOT NULL REFERENCES job_descriptions(id) ON DELETE CASCADE,
    score                INT NOT NULL CHECK (score BETWEEN 0 AND 100),
    matched_skills       JSONB NOT NULL DEFAULT '[]',
    missing_skills       JSONB NOT NULL DEFAULT '[]',
    weak_skills          JSONB NOT NULL DEFAULT '[]',
    recommendations      JSONB NOT NULL DEFAULT '[]',
    evidence             JSONB NOT NULL DEFAULT '[]',
    summary              VARCHAR(500),            -- one-line human-readable verdict
    provider             VARCHAR(20) NOT NULL,   -- ollama | openai
    latency_ms           INT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_analyses_user ON analyses (user_id, created_at DESC);
```

---

## 4. JSONB payload shapes

Stored on `analyses` so the verdict stays flexible without extra tables.

```jsonc
// matched_skills / missing_skills / weak_skills
[{ "skill": "Kafka", "importance": "HIGH", "evidenceRef": "RESUME#4" }]

// recommendations
[{ "text": "Add measurable impact to your Spring Boot bullet",
   "impact": "HIGH", "reason": "JD stresses production ownership" }]

// evidence
[{ "ref": "RESUME#4", "sourceType": "RESUME", "chunkIndex": 4,
   "snippet": "Built 8 REST endpoints ..." }]
```

`evidenceRef` values must resolve to an entry in `evidence` (enforced in the validation step, section 4.2 of `SYSTEM_ARCHITECTURE.md`).

---

## 5. JPA mapping notes

- **`document_chunks` (implemented, M4):** the `DocumentChunk` entity maps only the non-vector columns (`id`, `source_type`, `source_id`, `chunk_index`, `content`, `created_at`). The `embedding vector(768)` and `metadata jsonb` columns are **not** mapped as managed fields — chunks are written with a native `INSERT ... CAST(:embedding AS vector)` and read back for retrieval with a native cosine query (`1 - (embedding <=> CAST(:q AS vector))`, ordered by `<=>`). The embedding is passed as a bound, parameterized pgvector literal (`[0.1,0.2,...]`) — never string-concatenated. This keeps plain Hibernate (`ddl-auto: validate`) happy without a custom vector type; unmapped columns are allowed by `validate`.
- The single source of truth for the vector dimension is the `app.embedding.dimensions` config (default 768) — it must match both the embedding model and the `vector(N)` column.
- Alternative (deferred): a custom PGVector Hibernate type or Spring AI's `VectorStore` would remove the native SQL, at the cost of an added dependency. Revisit alongside the LLM synthesis work.
- JSONB columns (on `analyses`): map with `@JdbcTypeCode(SqlTypes.JSON)` on a typed field (list of records), or `hypersistence-utils`.
- Timestamps: `TIMESTAMPTZ` → `Instant`/`OffsetDateTime`.
- Always set `ON DELETE CASCADE` at DB level AND `orphanRemoval`/cascade in JPA to avoid orphans.

---

## 6. Migrations

- Use **Flyway**. Migrations in `backend/src/main/resources/db/migration/`.
- `V1__init.sql` — pgvector extension + core tables (users, resumes, job_descriptions, document_chunks, analyses)
- `V2__resume_metadata.sql` — adds resume metadata columns (content_type, file_size, file_path, page_count, language, deleted, updated_at) and partial index
- `V3__job_description_metadata.sql` — adds JD metadata columns (content_type, file_size, file_path, page_count, language, deleted, updated_at) and indexes (partial on active, GIN on title)
- Never edit an applied migration; add a new one.

---

## 7. Indexing & performance rules

- Vector search always filters by `source_type = 'RESUME'` (or JD) before/with ANN — keep `idx_chunks_source`.
- Cap top-k retrieval (e.g., k=5–8) to bound LLM prompt size.
- Cache embeddings: before embedding a document, check whether chunks already exist for `(source_type, source_id)`; skip if so.
- Paginate all list endpoints (resumes, JDs, analyses) — never unbounded `findAll`.
- **Chunk cleanup on document delete:** `document_chunks` uses a polymorphic `(source_type, source_id)` reference, so there is no hard FK cascade. The service layer must delete all chunks for a document *before* (or in the same transaction as) deleting the document itself. A scheduled cleanup job may run as a safety net to remove orphaned chunks.
- **Immutable-document design:** resumes and JDs are treated as immutable after upload. "Editing" means uploading a new version — this avoids invalidating cached embeddings.
