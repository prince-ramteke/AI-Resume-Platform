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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Retrieval for the analysis pipeline: locates the top-k most relevant chunks of a single
 * source document for a free-text query and returns them as ranked {@link ChunkEvidence}.
 *
 * <p>Two strategies, selected by {@link RagConfig#hybridEnabled()}:
 * <ul>
 *   <li><b>Vector-only (default):</b> embed the query and take the nearest chunks by cosine
 *       similarity. {@code score} carries the cosine similarity in [0, 1].</li>
 *   <li><b>Hybrid (v1.1):</b> run vector search and PostgreSQL full-text keyword search in
 *       parallel, then fuse the two rankings with Reciprocal Rank Fusion (RRF). This is a
 *       deterministic, rank-based re-rank — no ML re-ranker. {@code score} carries the RRF
 *       fused score. A chunk that only the keyword arm surfaces can still make the final set.</li>
 * </ul>
 *
 * <p>The synthesis/scoring over this evidence is downstream; this service only locates and
 * ranks candidates. Ownership is enforced upstream by the caller that resolves the
 * {@code sourceId}, so this service does not repeat access control. {@code ref}, {@code snippet},
 * {@code sourceType}, and {@code chunkIndex} are identical across both strategies, so grounding
 * and citation behavior are unaffected by the choice.
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
        boolean hybrid = ragConfig.hybridEnabled();
        log.info("Retrieval started: sourceType={}, sourceId={}, topK={}, hybrid={}",
                sourceType, sourceId, cappedTopK, hybrid);

        List<ChunkEvidence> evidence = hybrid
                ? hybridRetrieve(sourceType, sourceId, queryText, cappedTopK)
                : vectorRetrieve(sourceType, sourceId, queryText, cappedTopK);

        log.info("Retrieval finished: sourceType={}, sourceId={}, results={}",
                sourceType, sourceId, evidence.size());
        return evidence;
    }

    /** Vector-only path: nearest chunks by cosine similarity, score = cosine similarity. */
    private List<ChunkEvidence> vectorRetrieve(SourceType sourceType, Long sourceId,
                                               String queryText, int topK) {
        List<ChunkSimilarity> rows = vectorRows(sourceType, sourceId, queryText, topK);
        return rows.stream()
                .map(row -> toEvidence(row, row.getScore() != null ? row.getScore() : 0.0))
                .toList();
    }

    /**
     * Hybrid path: pull a candidate pool from each arm, fuse ranks with RRF, keep the top-k.
     * Pool size ({@link RagConfig#hybridCandidatePoolSize()}) is >= the final breadth so a
     * keyword-only match outside the vector top-k still has a chance to be promoted.
     */
    private List<ChunkEvidence> hybridRetrieve(SourceType sourceType, Long sourceId,
                                               String queryText, int topK) {
        int pool = ragConfig.hybridCandidatePoolSize();
        List<ChunkSimilarity> vectorRows = vectorRows(sourceType, sourceId, queryText, pool);
        List<ChunkSimilarity> keywordRows =
                chunkRepository.searchByKeyword(sourceType.name(), sourceId, queryText, pool);

        return fuseRrf(vectorRows, keywordRows, ragConfig.hybridRrfK(), topK);
    }

    private List<ChunkSimilarity> vectorRows(SourceType sourceType, Long sourceId,
                                             String queryText, int limit) {
        float[] queryVector = embeddingClient.embed(queryText);
        String queryLiteral = VectorFormat.toSqlString(queryVector);
        return chunkRepository.searchSimilar(sourceType.name(), sourceId, queryLiteral, limit);
    }

    /**
     * Reciprocal Rank Fusion. For each ranked list, a candidate at 1-based position {@code r}
     * contributes {@code 1 / (rrfK + r)}; a candidate's fused score is the sum across lists, so
     * appearing in both arms (or high in either) ranks it higher. Deterministic: identical
     * inputs yield identical output, and ties break by ascending chunk index for stability.
     * Candidates are keyed by chunk index (both arms are scoped to the same source document).
     */
    private List<ChunkEvidence> fuseRrf(List<ChunkSimilarity> vectorRows,
                                        List<ChunkSimilarity> keywordRows,
                                        int rrfK, int topK) {
        Map<Integer, Double> fusedScore = new LinkedHashMap<>();
        Map<Integer, ChunkSimilarity> byIndex = new LinkedHashMap<>();
        accumulate(vectorRows, rrfK, fusedScore, byIndex);
        accumulate(keywordRows, rrfK, fusedScore, byIndex);

        return fusedScore.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(topK)
                .map(entry -> toEvidence(byIndex.get(entry.getKey()), entry.getValue()))
                .toList();
    }

    private void accumulate(List<ChunkSimilarity> rows, int rrfK,
                            Map<Integer, Double> fusedScore,
                            Map<Integer, ChunkSimilarity> byIndex) {
        int rank = 1;
        for (ChunkSimilarity row : rows) {
            int index = row.getChunkIndex();
            fusedScore.merge(index, 1.0 / (rrfK + rank), Double::sum);
            byIndex.putIfAbsent(index, row);
            rank++;
        }
    }

    /** Map a chunk row to evidence with the given score, preserving ref/snippet/metadata. */
    private ChunkEvidence toEvidence(ChunkSimilarity row, double score) {
        SourceType sourceType = SourceType.valueOf(row.getSourceType());
        return new ChunkEvidence(
                row.getSourceType() + "#" + row.getChunkIndex(),
                sourceType,
                row.getChunkIndex(),
                row.getContent(),
                score);
    }
}
