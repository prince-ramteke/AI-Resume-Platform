package com.princeramteke.resumeai.rag.retrieval;

import com.princeramteke.resumeai.rag.chunk.SourceType;

/**
 * A retrieved chunk prepared as analysis evidence. {@code ref} is a stable citation handle
 * (e.g. {@code "RESUME#3"}) that later analysis output references so every claim is grounded
 * in a real chunk. Deliberately carries no embedding — raw vectors never cross a boundary.
 */
public record ChunkEvidence(
        String ref,
        SourceType sourceType,
        int chunkIndex,
        String snippet,
        double score
) {
}
