package com.princeramteke.resumeai.rag.ingestion;

import com.princeramteke.resumeai.rag.chunk.DocumentChunkRepository;
import com.princeramteke.resumeai.rag.chunk.SourceType;
import com.princeramteke.resumeai.rag.chunking.TextChunk;
import com.princeramteke.resumeai.rag.chunking.TextChunker;
import com.princeramteke.resumeai.rag.embedding.EmbeddingClient;
import com.princeramteke.resumeai.rag.embedding.VectorFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Turns a document's text into persisted, embedded chunks: chunk → embed → store.
 *
 * <p>Idempotent by design — if chunks already exist for the {@code (sourceType, sourceId)}
 * pair the work is skipped (embedding cache), so a document is never re-embedded. The
 * embedding call runs outside any database transaction; each chunk row is written by its own
 * transactional native insert, so a slow model call never holds a DB connection open.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final TextChunker chunker;
    private final EmbeddingClient embeddingClient;
    private final DocumentChunkRepository chunkRepository;

    public IngestionService(TextChunker chunker,
                            EmbeddingClient embeddingClient,
                            DocumentChunkRepository chunkRepository) {
        this.chunker = chunker;
        this.embeddingClient = embeddingClient;
        this.chunkRepository = chunkRepository;
    }

    /**
     * Chunk, embed and persist the given document text.
     *
     * @return the number of chunks persisted; 0 if already ingested or the text is empty.
     */
    public int ingest(SourceType sourceType, Long sourceId, String text) {
        if (chunkRepository.existsBySourceTypeAndSourceId(sourceType, sourceId)) {
            log.info("Ingestion skipped (already embedded): sourceType={}, sourceId={}", sourceType, sourceId);
            return 0;
        }

        log.info("Chunking started: sourceType={}, sourceId={}", sourceType, sourceId);
        List<TextChunk> chunks = chunker.chunk(text);
        log.info("Chunking finished: sourceType={}, sourceId={}, chunks={}", sourceType, sourceId, chunks.size());
        if (chunks.isEmpty()) {
            return 0;
        }

        log.info("Embedding started: sourceType={}, sourceId={}, chunks={}", sourceType, sourceId, chunks.size());
        List<float[]> embeddings = embeddingClient.embedBatch(
                chunks.stream().map(TextChunk::content).toList());
        log.info("Embedding finished: sourceType={}, sourceId={}", sourceType, sourceId);

        for (int i = 0; i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            chunkRepository.insertChunk(sourceType.name(), sourceId, chunk.index(),
                    chunk.content(), VectorFormat.toSqlString(embeddings.get(i)));
        }

        log.info("Ingestion finished: sourceType={}, sourceId={}, persisted={}",
                sourceType, sourceId, chunks.size());
        return chunks.size();
    }
}
