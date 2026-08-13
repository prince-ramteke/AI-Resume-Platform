package com.princeramteke.resumeai.rag.retrieval;

import com.princeramteke.resumeai.rag.RagConfig;
import com.princeramteke.resumeai.rag.chunk.ChunkSimilarity;
import com.princeramteke.resumeai.rag.chunk.DocumentChunkRepository;
import com.princeramteke.resumeai.rag.chunk.SourceType;
import com.princeramteke.resumeai.rag.embedding.EmbeddingClient;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetrievalServiceTest {

    @Mock
    private DocumentChunkRepository chunkRepository;
    @Mock
    private EmbeddingClient embeddingClient;

    private MeterRegistry meterRegistry;
    private RetrievalService service;

    private static final Long RESUME_ID = 7L;

    // ---------------------------------------------------------------------
    // Approved bounded tag universes (v1.2.M1). Tests assert emitted meters
    // never carry values outside these sets.
    // ---------------------------------------------------------------------
    private static final Set<String> ALLOWED_ARM_VALUES =
            Set.of("vector", "keyword", "fuse", "total", "both");
    private static final Set<String> ALLOWED_MODE_VALUES = Set.of("vector", "hybrid");
    private static final Set<String> ALLOWED_REASON_VALUES = Set.of("topk", "token_budget");

    @BeforeEach
    void setUp() {
        // Vector-only config (hybrid disabled) — the default, pre-hybrid behavior.
        meterRegistry = new SimpleMeterRegistry();
        service = new RetrievalService(chunkRepository, embeddingClient,
                new RagConfig(500, 50, 8, 3500), meterRegistry);
    }

    // ---------------------------------------------------------------------
    // Vector-only path (hybrid disabled)
    // ---------------------------------------------------------------------

    @Test
    void retrieve_mapsRowsToRankedEvidence() {
        // Build the projection mocks first — stubbing them inline inside the outer
        // when(...).thenReturn(...) would nest stubbing and fail.
        var row1 = similarity("RESUME", 2, "strong match", 0.91);
        var row2 = similarity("RESUME", 5, "weaker match", 0.42);
        when(embeddingClient.embed("job text")).thenReturn(new float[]{0.1f, 0.2f});
        when(chunkRepository.searchSimilar(eq("RESUME"), eq(RESUME_ID), anyString(), eq(8)))
                .thenReturn(List.of(row1, row2));

        List<ChunkEvidence> evidence = service.retrieve(SourceType.RESUME, RESUME_ID, "job text");

        assertThat(evidence).hasSize(2);
        assertThat(evidence.get(0).ref()).isEqualTo("RESUME#2");
        assertThat(evidence.get(0).sourceType()).isEqualTo(SourceType.RESUME);
        assertThat(evidence.get(0).chunkIndex()).isEqualTo(2);
        assertThat(evidence.get(0).snippet()).isEqualTo("strong match");
        assertThat(evidence.get(0).score()).isEqualTo(0.91);
        assertThat(evidence.get(1).ref()).isEqualTo("RESUME#5");
    }

    @Test
    void retrieve_hybridDisabled_neverRunsKeywordArm() {
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
        when(chunkRepository.searchSimilar(eq("RESUME"), eq(RESUME_ID), anyString(), eq(8)))
                .thenReturn(List.of());

        service.retrieve(SourceType.RESUME, RESUME_ID, "query");

        verify(chunkRepository, never()).searchByKeyword(anyString(), eq(RESUME_ID), anyString(), anyInt());
    }

    @Test
    void retrieve_cappsTopKAtConfiguredMaximum() {
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
        when(chunkRepository.searchSimilar(eq("RESUME"), eq(RESUME_ID), anyString(), eq(8)))
                .thenReturn(List.of());

        service.retrieve(SourceType.RESUME, RESUME_ID, "query", 100);

        // Requested 100, but config caps retrievalTopK at 8.
        verify(chunkRepository).searchSimilar(eq("RESUME"), eq(RESUME_ID), anyString(), eq(8));
    }

    @Test
    void retrieve_nullScore_defaultsToZero() {
        var row = similarity("RESUME", 0, "content", null);
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
        when(chunkRepository.searchSimilar(eq("RESUME"), eq(RESUME_ID), anyString(), eq(8)))
                .thenReturn(List.of(row));

        List<ChunkEvidence> evidence = service.retrieve(SourceType.RESUME, RESUME_ID, "query");

        assertThat(evidence.get(0).score()).isZero();
    }

    @Test
    void retrieve_noMatches_returnsEmptyList() {
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
        when(chunkRepository.searchSimilar(eq("RESUME"), eq(RESUME_ID), anyString(), eq(8)))
                .thenReturn(List.of());

        assertThat(service.retrieve(SourceType.RESUME, RESUME_ID, "query")).isEmpty();
    }

    // ---------------------------------------------------------------------
    // Hybrid path (vector + keyword, RRF fusion)
    // ---------------------------------------------------------------------

    @Test
    void retrieve_hybrid_mergesAndRanksByReciprocalRankFusion() {
        RetrievalService hybrid = hybridService();
        // Vector arm: chunk 2 (rank 1), chunk 5 (rank 2)
        // Keyword arm: chunk 5 (rank 1), chunk 9 (rank 2)  -> chunk 9 is keyword-only.
        // Build row mocks first — stubbing them inside the outer thenReturn(...) would nest stubbing.
        var vec2 = row(2, "vector strong");
        var both5 = row(5, "in both arms");
        var kw5 = row(5, "in both arms");
        var kw9 = row(9, "keyword only");
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
        when(chunkRepository.searchSimilar(eq("RESUME"), eq(RESUME_ID), anyString(), eq(20)))
                .thenReturn(List.of(vec2, both5));
        // "Java Spring Boot" → KeywordQueryBuilder produces "Java Spring Boot" (3 uppercase tokens)
        when(chunkRepository.searchByKeyword(eq("RESUME"), eq(RESUME_ID), eq("Java Spring Boot"), eq(20)))
                .thenReturn(List.of(kw5, kw9));

        List<ChunkEvidence> evidence = hybrid.retrieve(SourceType.RESUME, RESUME_ID, "Java Spring Boot");

        // RRF(k=60): chunk5 = 1/62 + 1/61 (both arms) > chunk2 = 1/61 (vector r1) > chunk9 = 1/62 (keyword r2).
        assertThat(evidence).extracting(ChunkEvidence::ref)
                .containsExactly("RESUME#5", "RESUME#2", "RESUME#9");
        // The chunk present in both arms scores strictly highest (deterministic fusion).
        assertThat(evidence.get(0).score()).isGreaterThan(evidence.get(1).score());
        assertThat(evidence.get(1).score()).isGreaterThan(evidence.get(2).score());
        // Metadata is preserved unchanged for grounding/citation.
        assertThat(evidence.get(0).snippet()).isEqualTo("in both arms");
        assertThat(evidence.get(0).sourceType()).isEqualTo(SourceType.RESUME);
        assertThat(evidence.get(0).chunkIndex()).isEqualTo(5);
    }

    @Test
    void retrieve_hybrid_keywordOnlyCandidateSurfaces() {
        RetrievalService hybrid = hybridService();
        var vec1 = row(1, "vector hit");
        var kw7 = row(7, "keyword hit the vector arm missed");
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
        when(chunkRepository.searchSimilar(eq("RESUME"), eq(RESUME_ID), anyString(), eq(20)))
                .thenReturn(List.of(vec1));
        when(chunkRepository.searchByKeyword(eq("RESUME"), eq(RESUME_ID), anyString(), eq(20)))
                .thenReturn(List.of(kw7));

        // Technical query → builder produces "Java Spring Boot" → keyword arm fires (anyString() matches)
        List<ChunkEvidence> evidence = hybrid.retrieve(SourceType.RESUME, RESUME_ID, "Java Spring Boot");

        assertThat(evidence).extracting(ChunkEvidence::ref)
                .containsExactlyInAnyOrder("RESUME#1", "RESUME#7");
    }

    @Test
    void retrieve_hybrid_scopesBothArmsToSameSource() {
        RetrievalService hybrid = hybridService();
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
        when(chunkRepository.searchSimilar(anyString(), eq(RESUME_ID), anyString(), anyInt()))
                .thenReturn(List.of());
        when(chunkRepository.searchByKeyword(anyString(), eq(RESUME_ID), anyString(), anyInt()))
                .thenReturn(List.of());

        // "Java Spring Boot" → builder extracts "Java Spring Boot" → both arms scoped to same source
        hybrid.retrieve(SourceType.RESUME, RESUME_ID, "Java Spring Boot");

        verify(chunkRepository).searchSimilar(eq("RESUME"), eq(RESUME_ID), anyString(), eq(20));
        verify(chunkRepository).searchByKeyword(eq("RESUME"), eq(RESUME_ID), eq("Java Spring Boot"), eq(20));
    }

    @Test
    void retrieve_hybrid_appliesFinalTopKLimit() {
        RetrievalService hybrid = hybridService();
        var r1 = row(1, "r1");
        var r2 = row(2, "r2");
        var r3 = row(3, "r3");
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
        // Three distinct vector candidates, keyword empty; request only top 2.
        when(chunkRepository.searchSimilar(eq("RESUME"), eq(RESUME_ID), anyString(), eq(20)))
                .thenReturn(List.of(r1, r2, r3));
        when(chunkRepository.searchByKeyword(eq("RESUME"), eq(RESUME_ID), anyString(), eq(20)))
                .thenReturn(List.of());

        List<ChunkEvidence> evidence = hybrid.retrieve(SourceType.RESUME, RESUME_ID, "Java Spring Boot", 2);

        // Truncated to the requested breadth, keeping the two highest RRF scores (ranks 1 and 2).
        assertThat(evidence).extracting(ChunkEvidence::ref).containsExactly("RESUME#1", "RESUME#2");
    }

    @Test
    void retrieve_hybrid_semanticProseQuery_keywordArmSkipped() {
        // "looking for a great developer to work with" → all stop words / lowercase → builder returns ""
        // → keyword arm must NOT fire (no DB round-trip, no unused stub exception)
        RetrievalService hybrid = hybridService();
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
        when(chunkRepository.searchSimilar(anyString(), eq(RESUME_ID), anyString(), anyInt()))
                .thenReturn(List.of());

        hybrid.retrieve(SourceType.RESUME, RESUME_ID, "looking for a great developer to work with");

        verify(chunkRepository, never()).searchByKeyword(anyString(), anyLong(), anyString(), anyInt());
    }

    @Test
    void retrieve_hybrid_lowercaseTechnicalVocab_keywordArmCalled() {
        // TECH_VOCAB covers common lowercase tech names; builder must keep them even without uppercase
        RetrievalService hybrid = hybridService();
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
        when(chunkRepository.searchSimilar(anyString(), eq(RESUME_ID), anyString(), anyInt()))
                .thenReturn(List.of());
        when(chunkRepository.searchByKeyword(eq("RESUME"), eq(RESUME_ID), eq("python kubernetes docker"), eq(20)))
                .thenReturn(List.of());

        hybrid.retrieve(SourceType.RESUME, RESUME_ID, "python kubernetes docker");

        verify(chunkRepository).searchByKeyword(eq("RESUME"), eq(RESUME_ID), eq("python kubernetes docker"), eq(20));
    }

    @Test
    void retrieve_hybrid_keywordQueryBoundedToTermLimit() {
        // hybridService() sets hybridKeywordTermLimit=5 (via 7-arg constructor default)
        // 8 uppercase tokens but only first 5 are passed to the keyword arm
        RetrievalService hybrid = hybridService();
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
        when(chunkRepository.searchSimilar(anyString(), eq(RESUME_ID), anyString(), anyInt()))
                .thenReturn(List.of());
        when(chunkRepository.searchByKeyword(eq("RESUME"), eq(RESUME_ID),
                eq("Java Spring Boot PostgreSQL Docker"), eq(20)))
                .thenReturn(List.of());

        hybrid.retrieve(SourceType.RESUME, RESUME_ID,
                "Java Spring Boot PostgreSQL Docker Redis Kafka Kubernetes");

        verify(chunkRepository).searchByKeyword(eq("RESUME"), eq(RESUME_ID),
                eq("Java Spring Boot PostgreSQL Docker"), eq(20));
    }

    // ---------------------------------------------------------------------
    // Observability (v1.2.M1) — assert metrics are emitted, not durations.
    // ---------------------------------------------------------------------

    @Test
    void observability_vectorOnly_recordsTotalAndVectorTimersOnly() {
        var hit = similarity("RESUME", 1, "hit", 0.9);
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
        when(chunkRepository.searchSimilar(eq("RESUME"), eq(RESUME_ID), anyString(), eq(8)))
                .thenReturn(List.of(hit));

        service.retrieve(SourceType.RESUME, RESUME_ID, "query");

        assertThat(timerCount("rag.retrieval.latency", "arm", "total", "mode", "vector")).isEqualTo(1);
        assertThat(timerCount("rag.retrieval.latency", "arm", "vector", "mode", "vector")).isEqualTo(1);
        // Keyword and fuse arms only exist in hybrid mode.
        assertThat(meterRegistry.find("rag.retrieval.latency").tag("arm", "keyword").timer()).isNull();
        assertThat(meterRegistry.find("rag.retrieval.latency").tag("arm", "fuse").timer()).isNull();
        // Vector candidate summary recorded once; keyword summary must not exist.
        assertThat(summaryCount("rag.retrieval.candidates", "arm", "vector")).isEqualTo(1);
        assertThat(meterRegistry.find("rag.retrieval.candidates").tag("arm", "keyword").summary()).isNull();
        // Hybrid-only metrics must not appear.
        assertThat(meterRegistry.find("rag.retrieval.overlap").summary()).isNull();
        assertThat(meterRegistry.find("rag.retrieval.fusion.winner").counter()).isNull();
        assertThat(meterRegistry.find("rag.retrieval.fusion.contribution").summary()).isNull();
    }

    @Test
    void observability_hybrid_recordsAllArmsAndFusionMetrics() {
        RetrievalService hybrid = hybridService();
        var vec1 = row(1, "vec only");
        var both3 = row(3, "in both");
        var kw3 = row(3, "in both");
        var kw9 = row(9, "keyword only");
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
        when(chunkRepository.searchSimilar(eq("RESUME"), eq(RESUME_ID), anyString(), eq(20)))
                .thenReturn(List.of(vec1, both3));
        when(chunkRepository.searchByKeyword(eq("RESUME"), eq(RESUME_ID), anyString(), eq(20)))
                .thenReturn(List.of(kw3, kw9));

        // "Java Spring Boot" → builder produces "Java Spring Boot" → keyword arm fires
        hybrid.retrieve(SourceType.RESUME, RESUME_ID, "Java Spring Boot");

        assertThat(timerCount("rag.retrieval.latency", "arm", "total", "mode", "hybrid")).isEqualTo(1);
        assertThat(timerCount("rag.retrieval.latency", "arm", "vector", "mode", "hybrid")).isEqualTo(1);
        assertThat(timerCount("rag.retrieval.latency", "arm", "keyword", "mode", "hybrid")).isEqualTo(1);
        assertThat(timerCount("rag.retrieval.latency", "arm", "fuse", "mode", "hybrid")).isEqualTo(1);

        assertThat(summaryCount("rag.retrieval.candidates", "arm", "vector")).isEqualTo(1);
        assertThat(summaryCount("rag.retrieval.candidates", "arm", "keyword")).isEqualTo(1);

        // Overlap = {3} → 1
        var overlap = meterRegistry.find("rag.retrieval.overlap").summary();
        assertThat(overlap).isNotNull();
        assertThat(overlap.count()).isEqualTo(1);
        assertThat(overlap.totalAmount()).isEqualTo(1.0);

        // Top-1 by RRF is chunk 3 (in both arms) — winner tag = both.
        assertThat(counterCount("rag.retrieval.fusion.winner", "arm", "both")).isEqualTo(1.0);
        assertThat(meterRegistry.find("rag.retrieval.fusion.winner").tag("arm", "vector").counter()).isNull();
        assertThat(meterRegistry.find("rag.retrieval.fusion.winner").tag("arm", "keyword").counter()).isNull();

        // Contribution for this call: 1 vector-only + 1 keyword-only + 1 both = totals 1,1,1.
        assertThat(summaryTotal("rag.retrieval.fusion.contribution", "arm", "vector")).isEqualTo(1.0);
        assertThat(summaryTotal("rag.retrieval.fusion.contribution", "arm", "keyword")).isEqualTo(1.0);
        assertThat(summaryTotal("rag.retrieval.fusion.contribution", "arm", "both")).isEqualTo(1.0);
    }

    @Test
    void observability_hybrid_droppedTopkCounterIncrementsWhenPoolExceedsTopK() {
        RetrievalService hybrid = hybridService();
        // Four distinct vector candidates, keyword empty, final topK=2 → 2 dropped by topk.
        var r1 = row(1, "r1");
        var r2 = row(2, "r2");
        var r3 = row(3, "r3");
        var r4 = row(4, "r4");
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
        when(chunkRepository.searchSimilar(eq("RESUME"), eq(RESUME_ID), anyString(), eq(20)))
                .thenReturn(List.of(r1, r2, r3, r4));
        when(chunkRepository.searchByKeyword(eq("RESUME"), eq(RESUME_ID), anyString(), eq(20)))
                .thenReturn(List.of());

        hybrid.retrieve(SourceType.RESUME, RESUME_ID, "Java Spring Boot", 2);

        assertThat(counterCount("rag.retrieval.dropped", "reason", "topk")).isEqualTo(2.0);
    }

    @Test
    void observability_tagValues_stayWithinApprovedUniverses() {
        // Drive the hybrid path once so every metric family emits.
        RetrievalService hybrid = hybridService();
        var v1 = row(1, "a");
        var v2 = row(2, "b");
        var v3 = row(3, "c");
        var k2 = row(2, "b");
        var k4 = row(4, "d");
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
        when(chunkRepository.searchSimilar(eq("RESUME"), eq(RESUME_ID), anyString(), eq(20)))
                .thenReturn(List.of(v1, v2, v3));
        when(chunkRepository.searchByKeyword(eq("RESUME"), eq(RESUME_ID), anyString(), eq(20)))
                .thenReturn(List.of(k2, k4));
        // "Java Spring Boot" → builder extracts 3 tokens → keyword arm fires (uses anyString() stub)
        hybrid.retrieve(SourceType.RESUME, RESUME_ID, "Java Spring Boot", 2);
        // And once through the vector path.
        var vhit = row(1, "a");
        when(chunkRepository.searchSimilar(eq("RESUME"), eq(RESUME_ID), anyString(), eq(8)))
                .thenReturn(List.of(vhit));
        service.retrieve(SourceType.RESUME, RESUME_ID, "query");

        for (Meter meter : meterRegistry.getMeters()) {
            String name = meter.getId().getName();
            if (!name.startsWith("rag.retrieval.")) {
                continue;
            }
            for (Tag tag : meter.getId().getTags()) {
                switch (tag.getKey()) {
                    case "arm" -> assertThat(ALLOWED_ARM_VALUES).as("arm on %s", name).contains(tag.getValue());
                    case "mode" -> assertThat(ALLOWED_MODE_VALUES).as("mode on %s", name).contains(tag.getValue());
                    case "reason" ->
                            assertThat(ALLOWED_REASON_VALUES).as("reason on %s", name).contains(tag.getValue());
                    default -> assertThat(tag.getKey())
                            .as("unexpected tag key on %s", name)
                            .isIn("arm", "mode", "reason");
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /** Hybrid config: enabled, standard RRF k=60, candidate pool 20 per arm. */
    private RetrievalService hybridService() {
        return new RetrievalService(chunkRepository, embeddingClient,
                new RagConfig(500, 50, 8, 3500, true, 60, 20), meterRegistry);
    }

    private ChunkSimilarity similarity(String sourceType, int index, String content, Double score) {
        ChunkSimilarity row = mock(ChunkSimilarity.class);
        when(row.getSourceType()).thenReturn(sourceType);
        when(row.getChunkIndex()).thenReturn(index);
        when(row.getContent()).thenReturn(content);
        when(row.getScore()).thenReturn(score);
        return row;
    }

    /**
     * RESUME row for hybrid tests. Stubs are lenient because a candidate pruned by the final
     * top-k limit never has its content/source read, and the keyword arm never reads score.
     */
    private ChunkSimilarity row(int index, String content) {
        ChunkSimilarity row = mock(ChunkSimilarity.class);
        lenient().when(row.getSourceType()).thenReturn("RESUME");
        lenient().when(row.getChunkIndex()).thenReturn(index);
        lenient().when(row.getContent()).thenReturn(content);
        return row;
    }

    private long timerCount(String name, String... tagKvs) {
        var timer = meterRegistry.find(name).tags(tagKvs).timer();
        return timer == null ? 0 : timer.count();
    }

    private long summaryCount(String name, String... tagKvs) {
        var summary = meterRegistry.find(name).tags(tagKvs).summary();
        return summary == null ? 0 : summary.count();
    }

    private double summaryTotal(String name, String... tagKvs) {
        var summary = meterRegistry.find(name).tags(tagKvs).summary();
        return summary == null ? 0.0 : summary.totalAmount();
    }

    private double counterCount(String name, String... tagKvs) {
        var counter = meterRegistry.find(name).tags(tagKvs).counter();
        return counter == null ? 0.0 : counter.count();
    }
}
