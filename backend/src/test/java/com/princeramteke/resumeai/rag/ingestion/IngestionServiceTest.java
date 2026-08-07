package com.princeramteke.resumeai.rag.ingestion;

import com.princeramteke.resumeai.rag.RagConfig;
import com.princeramteke.resumeai.rag.chunk.DocumentChunkRepository;
import com.princeramteke.resumeai.rag.chunk.SourceType;
import com.princeramteke.resumeai.rag.chunking.TextChunker;
import com.princeramteke.resumeai.rag.embedding.EmbeddingClient;
import com.princeramteke.resumeai.rag.embedding.FakeEmbeddingClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock
    private DocumentChunkRepository chunkRepository;

    private EmbeddingClient embeddingClient;
    private IngestionService service;

    private static final Long SOURCE_ID = 42L;

    @BeforeEach
    void setUp() {
        embeddingClient = spy(new FakeEmbeddingClient(4));
        TextChunker chunker = new TextChunker(new RagConfig(20, 5, 8, 3500));
        service = new IngestionService(chunker, embeddingClient, chunkRepository);
    }

    @Test
    void ingest_newDocument_chunksEmbedsAndPersists() {
        when(chunkRepository.existsBySourceTypeAndSourceId(SourceType.RESUME, SOURCE_ID))
                .thenReturn(false);
        String text = "The quick brown fox jumps over the lazy dog again and again and again.";

        int persisted = service.ingest(SourceType.RESUME, SOURCE_ID, text);

        assertThat(persisted).isGreaterThan(1);
        verify(embeddingClient).embedBatch(any());

        ArgumentCaptor<Integer> indexCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> embeddingCaptor = ArgumentCaptor.forClass(String.class);
        verify(chunkRepository, times(persisted)).insertChunk(
                eq("RESUME"), eq(SOURCE_ID), indexCaptor.capture(), anyString(), embeddingCaptor.capture());

        // Chunk indexes are sequential from 0, and each embedding is a pgvector literal.
        for (int i = 0; i < persisted; i++) {
            assertThat(indexCaptor.getAllValues().get(i)).isEqualTo(i);
            assertThat(embeddingCaptor.getAllValues().get(i)).startsWith("[").endsWith("]");
        }
    }

    @Test
    void ingest_alreadyEmbedded_skipsChunkingAndEmbedding() {
        when(chunkRepository.existsBySourceTypeAndSourceId(SourceType.JD, SOURCE_ID))
                .thenReturn(true);

        int persisted = service.ingest(SourceType.JD, SOURCE_ID, "some job description text");

        assertThat(persisted).isZero();
        verify(embeddingClient, never()).embedBatch(any());
        verify(chunkRepository, never()).insertChunk(anyString(), anyLong(), anyInt(), anyString(), anyString());
    }

    @Test
    void ingest_blankText_persistsNothing() {
        when(chunkRepository.existsBySourceTypeAndSourceId(SourceType.RESUME, SOURCE_ID))
                .thenReturn(false);

        int persisted = service.ingest(SourceType.RESUME, SOURCE_ID, "   ");

        assertThat(persisted).isZero();
        verify(embeddingClient, never()).embedBatch(any());
        verify(chunkRepository, never()).insertChunk(anyString(), anyLong(), anyInt(), anyString(), anyString());
    }
}
