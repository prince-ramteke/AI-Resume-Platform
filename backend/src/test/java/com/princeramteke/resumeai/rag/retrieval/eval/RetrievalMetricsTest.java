package com.princeramteke.resumeai.rag.retrieval.eval;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class RetrievalMetricsTest {

    // ---------------------------------------------------------------------
    // recallAtK
    // ---------------------------------------------------------------------

    @Test
    void recallAtK_emptyRelevantSet_throws() {
        assertThatThrownBy(() -> RetrievalMetrics.recallAtK(Set.of(), List.of(1, 2), 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recallAtK_nullRelevantSet_throws() {
        assertThatThrownBy(() -> RetrievalMetrics.recallAtK(null, List.of(1, 2), 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recallAtK_emptyRetrieved_returnsZero() {
        assertThat(RetrievalMetrics.recallAtK(Set.of(1), List.of(), 3)).isZero();
    }

    @Test
    void recallAtK_nullRetrieved_returnsZero() {
        assertThat(RetrievalMetrics.recallAtK(Set.of(1), null, 3)).isZero();
    }

    @Test
    void recallAtK_kZeroOrNegative_returnsZero() {
        assertThat(RetrievalMetrics.recallAtK(Set.of(1), List.of(1, 2, 3), 0)).isZero();
        assertThat(RetrievalMetrics.recallAtK(Set.of(1), List.of(1, 2, 3), -1)).isZero();
    }

    @Test
    void recallAtK_kExceedsRetrievedSize_examinesAvailablePrefix() {
        // relevant={1,3}, retrieved=[1,3]; k=10 → both hit, recall=2/2=1.0
        assertThat(RetrievalMetrics.recallAtK(Set.of(1, 3), List.of(1, 3), 10))
                .isCloseTo(1.0, within(1e-9));
    }

    @Test
    void recallAtK_singleRelevantAtRank1_isOne() {
        assertThat(RetrievalMetrics.recallAtK(Set.of(7), List.of(7, 2, 5), 3))
                .isCloseTo(1.0, within(1e-9));
    }

    @Test
    void recallAtK_singleRelevantAtLaterRank_withinK_isOne() {
        assertThat(RetrievalMetrics.recallAtK(Set.of(9), List.of(1, 2, 9, 4), 4))
                .isCloseTo(1.0, within(1e-9));
    }

    @Test
    void recallAtK_singleRelevantAtLaterRank_beyondK_isZero() {
        assertThat(RetrievalMetrics.recallAtK(Set.of(9), List.of(1, 2, 3, 9), 3)).isZero();
    }

    @Test
    void recallAtK_multipleRelevant_partialCoverage() {
        // relevant={2,5,8}, retrieved=[2,3,5,4,7], k=5 → hits at 2 and 5 → 2/3
        assertThat(RetrievalMetrics.recallAtK(Set.of(2, 5, 8), List.of(2, 3, 5, 4, 7), 5))
                .isCloseTo(2.0 / 3.0, within(1e-9));
    }

    @Test
    void recallAtK_multipleRelevant_fullCoverage() {
        assertThat(RetrievalMetrics.recallAtK(Set.of(1, 2, 3), List.of(1, 2, 3), 3))
                .isCloseTo(1.0, within(1e-9));
    }

    @Test
    void recallAtK_noHit_returnsZero() {
        assertThat(RetrievalMetrics.recallAtK(Set.of(9, 10), List.of(1, 2, 3), 3)).isZero();
    }

    // ---------------------------------------------------------------------
    // reciprocalRank
    // ---------------------------------------------------------------------

    @Test
    void reciprocalRank_emptyOrNullInputs_returnZero() {
        assertThat(RetrievalMetrics.reciprocalRank(Set.of(), List.of(1))).isZero();
        assertThat(RetrievalMetrics.reciprocalRank(Set.of(1), List.of())).isZero();
        assertThat(RetrievalMetrics.reciprocalRank(null, List.of(1))).isZero();
        assertThat(RetrievalMetrics.reciprocalRank(Set.of(1), null)).isZero();
    }

    @Test
    void reciprocalRank_relevantAtRank1_isOne() {
        assertThat(RetrievalMetrics.reciprocalRank(Set.of(4), List.of(4, 2, 3)))
                .isCloseTo(1.0, within(1e-9));
    }

    @Test
    void reciprocalRank_relevantAtRank2_isOneHalf() {
        assertThat(RetrievalMetrics.reciprocalRank(Set.of(4), List.of(9, 4, 3)))
                .isCloseTo(0.5, within(1e-9));
    }

    @Test
    void reciprocalRank_relevantAtRank3_isOneThird() {
        assertThat(RetrievalMetrics.reciprocalRank(Set.of(4), List.of(9, 8, 4)))
                .isCloseTo(1.0 / 3.0, within(1e-9));
    }

    @Test
    void reciprocalRank_multipleRelevant_usesFirstHit() {
        // First hit is at rank 2 (chunk 5); rank of 8 is ignored.
        assertThat(RetrievalMetrics.reciprocalRank(Set.of(5, 8), List.of(1, 5, 8)))
                .isCloseTo(0.5, within(1e-9));
    }

    @Test
    void reciprocalRank_noRelevantHit_isZero() {
        assertThat(RetrievalMetrics.reciprocalRank(Set.of(9), List.of(1, 2, 3))).isZero();
    }

    // ---------------------------------------------------------------------
    // mrr
    // ---------------------------------------------------------------------

    @Test
    void mrr_emptyOrNull_returnsZero() {
        assertThat(RetrievalMetrics.mrr(List.of())).isZero();
        assertThat(RetrievalMetrics.mrr(null)).isZero();
    }

    @Test
    void mrr_singleValue_returnsThatValue() {
        assertThat(RetrievalMetrics.mrr(List.of(0.5))).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void mrr_average() {
        // (1.0 + 0.5 + 0.0) / 3 = 0.5
        assertThat(RetrievalMetrics.mrr(List.of(1.0, 0.5, 0.0)))
                .isCloseTo(0.5, within(1e-9));
    }
}
