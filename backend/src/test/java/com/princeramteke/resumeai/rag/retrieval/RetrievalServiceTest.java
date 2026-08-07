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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
        service = new RetrievalService(chunkRepository, embeddingClient, new RagConfig(500, 50, 8, 3500));
    }

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

    private ChunkSimilarity similarity(String sourceType, int index, String content, Double score) {
        ChunkSimilarity row = mock(ChunkSimilarity.class);
        when(row.getSourceType()).thenReturn(sourceType);
        when(row.getChunkIndex()).thenReturn(index);
        when(row.getContent()).thenReturn(content);
        when(row.getScore()).thenReturn(score);
        return row;
    }
}
