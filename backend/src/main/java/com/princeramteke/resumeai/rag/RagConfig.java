package com.princeramteke.resumeai.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunable RAG parameters, bound from {@code app.rag.*}. Centralizes chunk sizing,
 * retrieval breadth, and the prompt token budget so they can be adjusted by
 * configuration rather than code.
 */
@ConfigurationProperties(prefix = "app.rag")
public record RagConfig(
        int chunkSize,
        int chunkOverlap,
        int retrievalTopK,
        int maxPromptTokens
) {
    /** Hard cap on retrieval breadth to bound LLM prompt size (see DATABASE.md: k <= 8). */
    private static final int MAX_TOP_K = 8;

    public RagConfig {
        if (chunkSize <= 0) {
            chunkSize = 500;
        }
        if (chunkOverlap < 0) {
            chunkOverlap = 50;
        }
        if (retrievalTopK <= 0) {
            retrievalTopK = 8;
        }
        if (maxPromptTokens <= 0) {
            maxPromptTokens = 3500;
        }
        if (chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException(
                    "app.rag.chunk-overlap (" + chunkOverlap
                            + ") must be smaller than app.rag.chunk-size (" + chunkSize + ")");
        }
        if (retrievalTopK > MAX_TOP_K) {
            retrievalTopK = MAX_TOP_K;
        }
    }
}
