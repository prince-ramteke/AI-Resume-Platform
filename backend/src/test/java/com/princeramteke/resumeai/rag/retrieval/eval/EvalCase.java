package com.princeramteke.resumeai.rag.retrieval.eval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Test-only record backing a single {@code rag-eval/cases/*.json} fixture consumed by
 * {@link RetrievalEvaluationHarnessIT}. Deliberately Jackson-friendly (canonical constructor,
 * property names match the JSON) and unknown fields are ignored so a fixture can carry extra
 * context (e.g., author notes) without breaking the harness.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EvalCase(
        String caseId,
        String sourceType,
        List<Chunk> chunks,
        String query,
        List<Integer> relevantChunkIndexes,
        String notes
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Chunk(int index, String content) {
    }
}
