-- V1__init.sql: Core schema for AI Resume Intelligence Platform
-- Extension + 5 tables + indexes

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. users
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_users_email ON users (email);

-- 2. resumes
CREATE TABLE resumes (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    filename   VARCHAR(255) NOT NULL,
    raw_text   TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_resumes_user ON resumes (user_id);

-- 3. job_descriptions
CREATE TABLE job_descriptions (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title      VARCHAR(255) NOT NULL,
    raw_text   TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_jd_user ON job_descriptions (user_id);

-- 4. document_chunks (pgvector)
CREATE TABLE document_chunks (
    id          BIGSERIAL PRIMARY KEY,
    source_type VARCHAR(10) NOT NULL,
    source_id   BIGINT      NOT NULL,
    chunk_index INT         NOT NULL,
    content     TEXT        NOT NULL,
    embedding   vector(768) NOT NULL,
    metadata    JSONB       NOT NULL DEFAULT '{}',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (source_type, source_id, chunk_index)
);

CREATE INDEX idx_chunks_embedding
    ON document_chunks
    USING hnsw (embedding vector_cosine_ops);

CREATE INDEX idx_chunks_source ON document_chunks (source_type, source_id);

-- 5. analyses
CREATE TABLE analyses (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    resume_id            BIGINT NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    job_description_id   BIGINT NOT NULL REFERENCES job_descriptions(id) ON DELETE CASCADE,
    score                INT NOT NULL CHECK (score BETWEEN 0 AND 100),
    summary              VARCHAR(500),
    matched_skills       JSONB NOT NULL DEFAULT '[]',
    missing_skills       JSONB NOT NULL DEFAULT '[]',
    weak_skills          JSONB NOT NULL DEFAULT '[]',
    recommendations      JSONB NOT NULL DEFAULT '[]',
    evidence             JSONB NOT NULL DEFAULT '[]',
    provider             VARCHAR(20) NOT NULL,
    latency_ms           INT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_analyses_user ON analyses (user_id, created_at DESC);
