package com.princeramteke.resumeai.rag.chunk;

/**
 * Read-only projection for a vector-similarity search hit. {@code score} is the cosine
 * similarity in [0, 1] (1 = closest), derived as {@code 1 - cosine_distance}.
 */
public interface ChunkSimilarity {
    Long getId();
    String getSourceType();
    Long getSourceId();
    Integer getChunkIndex();
    String getContent();
    Double getScore();
}
