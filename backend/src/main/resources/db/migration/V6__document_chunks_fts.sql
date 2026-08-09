-- V6__document_chunks_fts.sql: lexical (keyword) search support for hybrid retrieval.
--
-- Adds a GIN index over the English full-text vector of document_chunks.content so the
-- keyword arm of hybrid retrieval (RetrievalService + DocumentChunkRepository.searchByKeyword)
-- can match on terms rather than embedding proximity. No new column and no extension are
-- required: to_tsvector / plainto_tsquery / ts_rank are core PostgreSQL. The index expression
-- must match the query expression exactly ('english' regconfig) for the planner to use it.

CREATE INDEX idx_chunks_content_fts
    ON document_chunks
    USING gin (to_tsvector('english', content));
