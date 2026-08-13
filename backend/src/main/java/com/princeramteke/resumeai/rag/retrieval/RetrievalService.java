package com.princeramteke.resumeai.rag.retrieval;

import com.princeramteke.resumeai.rag.RagConfig;
import com.princeramteke.resumeai.rag.chunk.ChunkSimilarity;
import com.princeramteke.resumeai.rag.chunk.DocumentChunkRepository;
import com.princeramteke.resumeai.rag.chunk.SourceType;
import com.princeramteke.resumeai.rag.embedding.EmbeddingClient;
import com.princeramteke.resumeai.rag.embedding.VectorFormat;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 *
 * <p><b>Observability (v1.2.M1):</b> emits {@code rag.retrieval.latency} (tags {@code arm},
 * {@code mode}), {@code rag.retrieval.candidates} (tag {@code arm}), {@code rag.retrieval.overlap},
 * {@code rag.retrieval.fusion.winner}/{@code fusion.contribution} (tag {@code arm}), and
 * {@code rag.retrieval.dropped{reason=topk}}. All tag values are fixed enum-like strings —
 * no ids, no query text, no snippets. Instrumentation is purely additive: it does not alter
 * candidate ordering, scores, or the returned evidence list.
 */
@Service
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);

    // Metric name constants — kept private so tag universes stay small and inspectable.
    private static final String METRIC_LATENCY = "rag.retrieval.latency";
    private static final String METRIC_CANDIDATES = "rag.retrieval.candidates";
    private static final String METRIC_OVERLAP = "rag.retrieval.overlap";
    private static final String METRIC_WINNER = "rag.retrieval.fusion.winner";
    private static final String METRIC_CONTRIBUTION = "rag.retrieval.fusion.contribution";
    private static final String METRIC_DROPPED = "rag.retrieval.dropped";

    private static final String ARM_VECTOR = "vector";
    private static final String ARM_KEYWORD = "keyword";
    private static final String ARM_FUSE = "fuse";
    private static final String ARM_TOTAL = "total";
    private static final String ARM_BOTH = "both";
    private static final String MODE_VECTOR = "vector";
    private static final String MODE_HYBRID = "hybrid";
    private static final String REASON_TOPK = "topk";

    private final DocumentChunkRepository chunkRepository;
    private final EmbeddingClient embeddingClient;
    private final RagConfig ragConfig;
    private final MeterRegistry meterRegistry;

    public RetrievalService(DocumentChunkRepository chunkRepository,
                            EmbeddingClient embeddingClient,
                            RagConfig ragConfig,
                            MeterRegistry meterRegistry) {
        this.chunkRepository = chunkRepository;
        this.embeddingClient = embeddingClient;
        this.ragConfig = ragConfig;
        this.meterRegistry = meterRegistry;
    }

    /** Retrieve using the configured default top-k. */
    public List<ChunkEvidence> retrieve(SourceType sourceType, Long sourceId, String queryText) {
        return retrieve(sourceType, sourceId, queryText, ragConfig.retrievalTopK());
    }

    public List<ChunkEvidence> retrieve(SourceType sourceType, Long sourceId, String queryText, int topK) {
        int cappedTopK = Math.min(topK, ragConfig.retrievalTopK());
        boolean hybrid = ragConfig.hybridEnabled();
        String mode = hybrid ? MODE_HYBRID : MODE_VECTOR;
        log.info("Retrieval started: sourceType={}, sourceId={}, topK={}, hybrid={}",
                sourceType, sourceId, cappedTopK, hybrid);

        Timer.Sample totalSample = Timer.start(meterRegistry);
        try {
            List<ChunkEvidence> evidence = hybrid
                    ? hybridRetrieve(sourceType, sourceId, queryText, cappedTopK)
                    : vectorRetrieve(sourceType, sourceId, queryText, cappedTopK, mode);
            log.info("Retrieval finished: sourceType={}, sourceId={}, results={}",
                    sourceType, sourceId, evidence.size());
            return evidence;
        } finally {
            totalSample.stop(latencyTimer(ARM_TOTAL, mode));
        }
    }

    /** Vector-only path: nearest chunks by cosine similarity, score = cosine similarity. */
    private List<ChunkEvidence> vectorRetrieve(SourceType sourceType, Long sourceId,
                                               String queryText, int topK, String mode) {
        List<ChunkSimilarity> rows = vectorRows(sourceType, sourceId, queryText, topK, mode);
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
        List<ChunkSimilarity> vectorRows = vectorRows(sourceType, sourceId, queryText, pool, MODE_HYBRID);

        // Derive a short, bounded FTS query from the full query text. Passing the raw JD text to
        // plainto_tsquery generates a 15–40 term AND-conjunction that no single chunk satisfies,
        // causing 0 keyword candidates. KeywordQueryBuilder extracts up to N distinctive technical
        // tokens; if none qualify (semantic/prose JD), we skip the keyword arm entirely.
        String keywordQuery = KeywordQueryBuilder.build(queryText, ragConfig.hybridKeywordTermLimit());
        List<ChunkSimilarity> keywordRows;
        if (keywordQuery.isBlank()) {
            log.debug("Keyword arm skipped: no distinctive technical terms extracted from query");
            keywordRows = List.of();
        } else {
            Timer.Sample kwSample = Timer.start(meterRegistry);
            try {
                keywordRows = chunkRepository.searchByKeyword(
                        sourceType.name(), sourceId, keywordQuery, pool);
            } finally {
                kwSample.stop(latencyTimer(ARM_KEYWORD, MODE_HYBRID));
            }
        }
        candidatesSummary(ARM_KEYWORD).record(keywordRows.size());

        // Origin sets are captured BEFORE fusion so downstream winner/contribution
        // classification reflects which arm actually surfaced a chunk.
        Set<Integer> vectorIdx = indexSet(vectorRows);
        Set<Integer> keywordIdx = indexSet(keywordRows);
        Set<Integer> overlap = new HashSet<>(vectorIdx);
        overlap.retainAll(keywordIdx);
        overlapSummary().record(overlap.size());

        Timer.Sample fuseSample = Timer.start(meterRegistry);
        List<ChunkEvidence> fusedAll;
        try {
            fusedAll = fuseRrfAll(vectorRows, keywordRows, ragConfig.hybridRrfK());
        } finally {
            fuseSample.stop(latencyTimer(ARM_FUSE, MODE_HYBRID));
        }

        List<ChunkEvidence> topResults = fusedAll.size() > topK
                ? fusedAll.subList(0, topK)
                : fusedAll;

        int dropped = Math.max(0, fusedAll.size() - topK);
        if (dropped > 0) {
            droppedCounter(REASON_TOPK).increment(dropped);
        }

        recordFusionClassification(topResults, vectorIdx, keywordIdx);
        return topResults;
    }

    private List<ChunkSimilarity> vectorRows(SourceType sourceType, Long sourceId,
                                             String queryText, int limit, String mode) {
        // Embedding is a network/compute call — kept OUTSIDE the vector-arm timer so the
        // timer measures the SQL search alone, matching the keyword-arm scope.
        float[] queryVector = embeddingClient.embed(queryText);
        String queryLiteral = VectorFormat.toSqlString(queryVector);
        Timer.Sample sample = Timer.start(meterRegistry);
        List<ChunkSimilarity> rows;
        try {
            rows = chunkRepository.searchSimilar(sourceType.name(), sourceId, queryLiteral, limit);
        } finally {
            sample.stop(latencyTimer(ARM_VECTOR, mode));
        }
        candidatesSummary(ARM_VECTOR).record(rows.size());
        return rows;
    }

    /**
     * Reciprocal Rank Fusion, returning the fully sorted candidate list (no truncation).
     * For each ranked list, a candidate at 1-based position {@code r} contributes
     * {@code 1 / (rrfK + r)}; a candidate's fused score is the sum across lists, so appearing
     * in both arms (or high in either) ranks it higher. Deterministic: identical inputs yield
     * identical output, and ties break by ascending chunk index for stability. Candidates are
     * keyed by chunk index (both arms are scoped to the same source document).
     */
    private List<ChunkEvidence> fuseRrfAll(List<ChunkSimilarity> vectorRows,
                                           List<ChunkSimilarity> keywordRows,
                                           int rrfK) {
        Map<Integer, Double> fusedScore = new LinkedHashMap<>();
        Map<Integer, ChunkSimilarity> byIndex = new LinkedHashMap<>();
        accumulate(vectorRows, rrfK, fusedScore, byIndex);
        accumulate(keywordRows, rrfK, fusedScore, byIndex);

        return fusedScore.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
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

    // ---------------------------------------------------------------------
    // Metric helpers — small, private, purely additive. Tag universes are
    // fixed enum-like strings (see class-level Observability note).
    // ---------------------------------------------------------------------

    private Timer latencyTimer(String arm, String mode) {
        return Timer.builder(METRIC_LATENCY)
                .description("Retrieval phase latency, tagged by arm and retrieval mode")
                .tag("arm", arm)
                .tag("mode", mode)
                .register(meterRegistry);
    }

    private DistributionSummary candidatesSummary(String arm) {
        return DistributionSummary.builder(METRIC_CANDIDATES)
                .description("Candidates returned by a retrieval arm before fusion/truncation")
                .tag("arm", arm)
                .register(meterRegistry);
    }

    private DistributionSummary overlapSummary() {
        return DistributionSummary.builder(METRIC_OVERLAP)
                .description("Chunk-index overlap between the vector and keyword candidate pools (hybrid only)")
                .register(meterRegistry);
    }

    private Counter winnerCounter(String arm) {
        return Counter.builder(METRIC_WINNER)
                .description("Origin arm of the top-1 hybrid result")
                .tag("arm", arm)
                .register(meterRegistry);
    }

    private DistributionSummary contributionSummary(String arm) {
        return DistributionSummary.builder(METRIC_CONTRIBUTION)
                .description("Count of final top-k hybrid results attributable to each arm")
                .tag("arm", arm)
                .register(meterRegistry);
    }

    private Counter droppedCounter(String reason) {
        return Counter.builder(METRIC_DROPPED)
                .description("Fused candidates dropped from the final result set, tagged by reason")
                .tag("reason", reason)
                .register(meterRegistry);
    }

    private static Set<Integer> indexSet(List<ChunkSimilarity> rows) {
        Set<Integer> out = new HashSet<>(rows.size() * 2);
        for (ChunkSimilarity row : rows) {
            out.add(row.getChunkIndex());
        }
        return out;
    }

    private void recordFusionClassification(List<ChunkEvidence> topResults,
                                            Set<Integer> vectorIdx,
                                            Set<Integer> keywordIdx) {
        if (topResults.isEmpty()) {
            return;
        }
        int top1 = topResults.get(0).chunkIndex();
        winnerCounter(classify(top1, vectorIdx, keywordIdx)).increment();

        int v = 0, k = 0, b = 0;
        for (ChunkEvidence e : topResults) {
            switch (classify(e.chunkIndex(), vectorIdx, keywordIdx)) {
                case ARM_VECTOR -> v++;
                case ARM_KEYWORD -> k++;
                case ARM_BOTH -> b++;
                default -> {
                    // exhaustive: classify only returns the three known arms.
                }
            }
        }
        contributionSummary(ARM_VECTOR).record(v);
        contributionSummary(ARM_KEYWORD).record(k);
        contributionSummary(ARM_BOTH).record(b);
    }

    private static String classify(int chunkIndex, Set<Integer> vectorIdx, Set<Integer> keywordIdx) {
        boolean inV = vectorIdx.contains(chunkIndex);
        boolean inK = keywordIdx.contains(chunkIndex);
        if (inV && inK) {
            return ARM_BOTH;
        }
        if (inV) {
            return ARM_VECTOR;
        }
        // Fused chunks always originate from at least one arm; if it isn't the vector arm,
        // it must be the keyword arm.
        return ARM_KEYWORD;
    }
}
