package com.princeramteke.resumeai.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * Tunable RAG parameters, bound from {@code app.rag.*}. Centralizes chunk sizing,
 * retrieval breadth, the prompt token budget, and hybrid-retrieval fusion settings
 * so they can be adjusted by configuration rather than code.
 *
 * <p><b>Hybrid retrieval (v1.1):</b> when {@code hybridEnabled} is true, retrieval blends
 * vector similarity with PostgreSQL full-text (keyword) search and fuses the two rankings
 * with Reciprocal Rank Fusion. It defaults to {@code false} so the shipped behavior stays
 * vector-only until deliberately enabled.
 *
 * <p><b>Keyword query bounding (v1.2.M2):</b> {@code hybridKeywordTermLimit} caps how many
 * distinctive technical tokens {@link com.princeramteke.resumeai.rag.retrieval.KeywordQueryBuilder}
 * extracts from the query. Passing the full JD text to {@code plainto_tsquery} generates a 15+
 * term AND-conjunction that no single chunk can satisfy; bounding to 2–5 terms fixes the bug
 * where the keyword arm returned 0 candidates for realistic JD inputs.
 */
@ConfigurationProperties(prefix = "app.rag")
public record RagConfig(
        int chunkSize,
        int chunkOverlap,
        int retrievalTopK,
        int maxPromptTokens,
        boolean hybridEnabled,
        int hybridRrfK,
        int hybridCandidatePoolSize,
        int hybridKeywordTermLimit
) {
    /** Hard cap on retrieval breadth to bound LLM prompt size (see DATABASE.md: k <= 8). */
    private static final int MAX_TOP_K = 8;
    /** Maximum allowed keyword term limit — avoids generating overly long AND-conjunctions. */
    private static final int MAX_KEYWORD_TERM_LIMIT = 10;

    // Disambiguates for Spring Boot @ConfigurationProperties binding: this record has
    // extra backward-compat constructors, and without an explicit marker the binder
    // can't tell which one to use and falls back to no-arg instantiation (fails on records).
    @ConstructorBinding
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
        // RRF constant; the standard default is 60. Non-positive is meaningless (would divide
        // by the rank alone or blow up), so fall back to the canonical value.
        if (hybridRrfK <= 0) {
            hybridRrfK = 60;
        }
        // How many candidates to pull from EACH of the vector and keyword lists before fusing.
        // Must be at least the final breadth so fusion has room to promote keyword-only hits.
        if (hybridCandidatePoolSize < retrievalTopK) {
            hybridCandidatePoolSize = Math.max(retrievalTopK, 20);
        }
        // Keyword term limit: default 5 if not configured; cap at 10 to stay within a
        // manageable AND-conjunction for plainto_tsquery.
        if (hybridKeywordTermLimit <= 0) {
            hybridKeywordTermLimit = 5;
        }
        if (hybridKeywordTermLimit > MAX_KEYWORD_TERM_LIMIT) {
            hybridKeywordTermLimit = MAX_KEYWORD_TERM_LIMIT;
        }
    }

    /**
     * Backward-compatible constructor for the 7-parameter hybrid set (pre-v1.2.M2).
     * Defaults {@code hybridKeywordTermLimit} to 5. Kept so existing tests and callers
     * that predate the keyword-term-limit setting continue to compile unchanged.
     */
    public RagConfig(int chunkSize, int chunkOverlap, int retrievalTopK, int maxPromptTokens,
                     boolean hybridEnabled, int hybridRrfK, int hybridCandidatePoolSize) {
        this(chunkSize, chunkOverlap, retrievalTopK, maxPromptTokens,
                hybridEnabled, hybridRrfK, hybridCandidatePoolSize, 5);
    }

    /**
     * Backward-compatible constructor for the pre-hybrid (4-parameter) set. Defaults hybrid
     * retrieval OFF, preserving the original vector-only behavior. Kept so existing callers
     * (and tests) that predate the hybrid settings continue to compile unchanged.
     */
    public RagConfig(int chunkSize, int chunkOverlap, int retrievalTopK, int maxPromptTokens) {
        this(chunkSize, chunkOverlap, retrievalTopK, maxPromptTokens, false, 60, 20, 5);
    }
}
