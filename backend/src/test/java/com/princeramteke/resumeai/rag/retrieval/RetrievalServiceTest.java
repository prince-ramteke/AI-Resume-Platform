package com.princeramteke.resumeai.rag.retrieval;

import com.princeramteke.resumeai.rag.RagConfig;
import com.princeramteke.resumeai.rag.chunk.ChunkSimilarity;
import com.princeramteke.resumeai.rag.chunk.DocumentChunkRepository;
import com.princeramteke.resumeai.rag.chunk.SourceType;
import com.princeramteke.resumeai.rag.embedding.EmbeddingClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
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

    private RetrievalService service;

    private static final Long RESUME_ID = 7L;

    @BeforeEach
    void setUp() {
        // Vector-only config (hybrid disabled) — the default, pre-hybrid behavior.
        service = new RetrievalService(chunkRepository, embeddingClient, new RagConfig(500, 50, 8, 3500));
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
        when(embeddingClient.embed("job text")).thenReturn(new float[]{0.1f});
        when(chunkRepository.searchSimilar(eq("RESUME"), eq(RESUME_ID), anyString(), eq(20)))
                .thenReturn(List.of(vec2, both5));
        when(chunkRepository.searchByKeyword(eq("RESUME"), eq(RESUME_ID), eq("job text"), eq(20)))
                .thenReturn(List.of(kw5, kw9));

        List<ChunkEvidence> evidence = hybrid.retrieve(SourceType.RESUME, RESUME_ID, "job text");

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

        List<ChunkEvidence> evidence = hybrid.retrieve(SourceType.RESUME, RESUME_ID, "query");

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

        hybrid.retrieve(SourceType.RESUME, RESUME_ID, "job text");

        verify(chunkRepository).searchSimilar(eq("RESUME"), eq(RESUME_ID), anyString(), eq(20));
        verify(chunkRepository).searchByKeyword(eq("RESUME"), eq(RESUME_ID), eq("job text"), eq(20));
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

        List<ChunkEvidence> evidence = hybrid.retrieve(SourceType.RESUME, RESUME_ID, "query", 2);

        // Truncated to the requested breadth, keeping the two highest RRF scores (ranks 1 and 2).
        assertThat(evidence).extracting(ChunkEvidence::ref).containsExactly("RESUME#1", "RESUME#2");
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /** Hybrid config: enabled, standard RRF k=60, candidate pool 20 per arm. */
    private RetrievalService hybridService() {
        return new RetrievalService(chunkRepository, embeddingClient,
                new RagConfig(500, 50, 8, 3500, true, 60, 20));
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
}
