-- V7__embedding_dimension_1536.sql
-- Widens the embedding column from vector(768) to vector(1536) to support
-- OpenAI text-embedding-3-small (1536 dims) as the production embedding provider.
-- Safe on a fresh DB (empty table) and also safe on a populated local DB only if
-- all existing chunks are deleted and re-embedded after applying this migration.
-- Production DB is expected to be fresh; no data migration is required there.

ALTER TABLE document_chunks ALTER COLUMN embedding TYPE vector(1536);

-- The HNSW index is dimension-specific and must be rebuilt after the type change.
DROP INDEX IF EXISTS idx_chunks_embedding;
CREATE INDEX idx_chunks_embedding
    ON document_chunks
    USING hnsw (embedding vector_cosine_ops);
