package com.princeramteke.resumeai.rag.chunk;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Data access for {@code document_chunks}. Writes and vector searches use native SQL
 * because the {@code embedding} column is a pgvector type: the embedding is passed as a
 * parameterized literal and cast with {@code CAST(:x AS vector)} (no string concatenation),
 * and similarity uses the cosine-distance operator {@code <=>}.
 */
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    /** Embedding cache check: true if this document has already been chunked and embedded. */
    boolean existsBySourceTypeAndSourceId(SourceType sourceType, Long sourceId);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO document_chunks (source_type, source_id, chunk_index, content, embedding)
            VALUES (:sourceType, :sourceId, :chunkIndex, :content, CAST(:embedding AS vector))
            """, nativeQuery = true)
    void insertChunk(@Param("sourceType") String sourceType,
                     @Param("sourceId") Long sourceId,
                     @Param("chunkIndex") int chunkIndex,
                     @Param("content") String content,
                     @Param("embedding") String embedding);

    /**
     * Top-k nearest chunks of one document to a query embedding, closest first.
     * Filters by {@code source_type} + {@code source_id} alongside the ANN search.
     */
    @Query(value = """
            SELECT id AS id,
                   source_type AS sourceType,
                   source_id AS sourceId,
                   chunk_index AS chunkIndex,
                   content AS content,
                   1 - (embedding <=> CAST(:queryVector AS vector)) AS score
            FROM document_chunks
            WHERE source_type = :sourceType AND source_id = :sourceId
            ORDER BY embedding <=> CAST(:queryVector AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<ChunkSimilarity> searchSimilar(@Param("sourceType") String sourceType,
                                        @Param("sourceId") Long sourceId,
                                        @Param("queryVector") String queryVector,
                                        @Param("topK") int topK);

    /**
     * Top-k lexical (keyword) matches of one document to a free-text query, best first.
     * Uses PostgreSQL full-text search: {@code plainto_tsquery} parses the query, the GIN
     * index on {@code to_tsvector('english', content)} (see V6 migration) serves the match,
     * and {@code ts_rank} scores relevance. Same {@code source_type}+{@code source_id}
     * scoping as {@link #searchSimilar} so hybrid retrieval blends like-for-like candidates.
     * The {@code score} here is a lexical rank, not a cosine similarity; hybrid fusion uses
     * only the row's rank position, so the two score scales never need to be comparable.
     */
    @Query(value = """
            SELECT id AS id,
                   source_type AS sourceType,
                   source_id AS sourceId,
                   chunk_index AS chunkIndex,
                   content AS content,
                   ts_rank(to_tsvector('english', content), plainto_tsquery('english', :query)) AS score
            FROM document_chunks
            WHERE source_type = :sourceType AND source_id = :sourceId
              AND to_tsvector('english', content) @@ plainto_tsquery('english', :query)
            ORDER BY score DESC, chunk_index ASC
            LIMIT :topK
            """, nativeQuery = true)
    List<ChunkSimilarity> searchByKeyword(@Param("sourceType") String sourceType,
                                          @Param("sourceId") Long sourceId,
                                          @Param("query") String query,
                                          @Param("topK") int topK);
}
