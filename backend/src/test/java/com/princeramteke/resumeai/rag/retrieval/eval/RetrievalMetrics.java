package com.princeramteke.resumeai.rag.retrieval.eval;

import java.util.List;
import java.util.Set;

/**
 * Pure, deterministic information-retrieval metrics used by the M1 evaluation harness.
 * Test-scope only: no I/O, no logging, no framework dependencies.
 *
 * <p>Definitions match the approved v1.2.M1 planning report:
 * <ul>
 *   <li><b>Recall@K</b> = {@code |relevant ∩ retrieved[0..K)| / |relevant|}. Undefined when
 *       {@code relevant} is empty — callers must skip such cases before calling.</li>
 *   <li><b>Reciprocal Rank</b> = {@code 1 / rank} of the first retrieved item that is in
 *       {@code relevant} (1-based); {@code 0} if none appear.</li>
 *   <li><b>MRR</b> = arithmetic mean of the supplied reciprocal ranks. {@code 0} for an
 *       empty list.</li>
 * </ul>
 */
public final class RetrievalMetrics {

    private RetrievalMetrics() {
    }

    /**
     * Recall at rank {@code k}. Throws {@link IllegalArgumentException} when the relevant set
     * is empty because the metric is undefined there — the harness pre-filters such cases.
     * Returns {@code 0.0} when {@code k <= 0} or the retrieved list is empty. If {@code k}
     * exceeds the retrieved list size, only the available prefix is examined.
     */
    public static double recallAtK(Set<Integer> relevant, List<Integer> retrieved, int k) {
        if (relevant == null || relevant.isEmpty()) {
            throw new IllegalArgumentException(
                    "recallAtK is undefined when the relevant set is empty; skip the case");
        }
        if (retrieved == null || retrieved.isEmpty() || k <= 0) {
            return 0.0;
        }
        int limit = Math.min(k, retrieved.size());
        int hits = 0;
        for (int i = 0; i < limit; i++) {
            if (relevant.contains(retrieved.get(i))) {
                hits++;
            }
        }
        return (double) hits / (double) relevant.size();
    }

    /**
     * Reciprocal rank of the first relevant hit (1-based). Returns {@code 0.0} when either
     * input is empty/null or no relevant item appears in {@code retrieved}.
     */
    public static double reciprocalRank(Set<Integer> relevant, List<Integer> retrieved) {
        if (relevant == null || relevant.isEmpty() || retrieved == null || retrieved.isEmpty()) {
            return 0.0;
        }
        for (int i = 0; i < retrieved.size(); i++) {
            if (relevant.contains(retrieved.get(i))) {
                return 1.0 / (i + 1);
            }
        }
        return 0.0;
    }

    /** Arithmetic mean of reciprocal ranks; {@code 0.0} for empty/null input. */
    public static double mrr(List<Double> reciprocalRanks) {
        if (reciprocalRanks == null || reciprocalRanks.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (double rr : reciprocalRanks) {
            sum += rr;
        }
        return sum / reciprocalRanks.size();
    }
}
