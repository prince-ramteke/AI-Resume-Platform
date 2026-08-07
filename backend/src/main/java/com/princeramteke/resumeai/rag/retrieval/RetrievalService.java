package com.princeramteke.resumeai.rag.retrieval;

import com.princeramteke.resumeai.rag.RagConfig;
import com.princeramteke.resumeai.rag.chunk.ChunkSimilarity;
import com.princeramteke.resumeai.rag.chunk.DocumentChunkRepository;
import com.princeramteke.resumeai.rag.chunk.SourceType;
import com.princeramteke.resumeai.rag.embedding.EmbeddingClient;
import com.princeramteke.resumeai.rag.embedding.VectorFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Retrieval groundwork for the analysis pipeline: embeds a free-text query and returns the
 * top-k most similar chunks of a single source document as ranked {@link ChunkEvidence}.
 * The scoring/synthesis over this evidence is a later milestone; this service only locates
 * and ranks candidates. Ownership is enforced upstream by the caller that resolves the
 * {@code sourceId}, so this service does not repeat access control.
 */
@Service
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);

    private final DocumentChunkRepository chunkRepository;
    private final EmbeddingClient embeddingClient;
    private final RagConfig ragConfig;

    public RetrievalService(DocumentChunkRepository chunkRepository,
                            EmbeddingClient embeddingClient,
                            RagConfig ragConfig) {
        this.chunkRepository = chunkRepository;
        this.embeddingClient = embeddingClient;
        this.ragConfig = ragConfig;
    }

    /** Retrieve using the configured default top-k. */
    public List<ChunkEvidence> retrieve(SourceType sourceType, Long sourceId, String queryText) {
        return retrieve(sourceType, sourceId, queryText, ragConfig.retrievalTopK());
    }

    public List<ChunkEvidence> retrieve(SourceType sourceType, Long sourceId, String queryText, int topK) {
        int cappedTopK = Math.min(topK, ragConfig.retrievalTopK());
        log.info("Retrieval started: sourceType={}, sourceId={}, topK={}", sourceType, sourceId, cappedTopK);

        float[] queryVector = embeddingClient.embed(queryText);
        String queryLiteral = VectorFormat.toSqlString(queryVector);

        List<ChunkSimilarity> rows = chunkRepository.searchSimilar(
                sourceType.name(), sourceId, queryLiteral, cappedTopK);

        List<ChunkEvidence> evidence = rows.stream()
                .map(row -> new ChunkEvidence(
                        row.getSourceType() + "#" + row.getChunkIndex(),
                        SourceType.valueOf(row.getSourceType()),
                        row.getChunkIndex(),
                        row.getContent(),
                        row.getScore() != null ? row.getScore() : 0.0))
                .toList();

        log.info("Retrieval finished: sourceType={}, sourceId={}, results={}",
                sourceType, sourceId, evidence.size());
        return evidence;
    }
}
