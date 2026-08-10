package com.princeramteke.resumeai.rag.retrieval.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.princeramteke.resumeai.rag.RagConfig;
import com.princeramteke.resumeai.rag.chunk.DocumentChunkRepository;
import com.princeramteke.resumeai.rag.chunk.SourceType;
import com.princeramteke.resumeai.rag.embedding.EmbeddingClient;
import com.princeramteke.resumeai.rag.embedding.FakeEmbeddingClient;
import com.princeramteke.resumeai.rag.embedding.VectorFormat;
import com.princeramteke.resumeai.rag.retrieval.ChunkEvidence;
import com.princeramteke.resumeai.rag.retrieval.RetrievalService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * v1.2.M1 retrieval evaluation harness.
 *
 * <p>Loads static JSON fixtures from {@code classpath:rag-eval/cases/*.json}, ingests each
 * into a per-run Testcontainers Postgres (pgvector image) using {@link FakeEmbeddingClient}
 * for deterministic embeddings, and runs {@link RetrievalService} in both vector-only and
 * hybrid modes at {@code k = {3, 5, 8}}. Reports aggregated Recall@K and MRR per mode/k.
 *
 * <p><b>Not a quality gate.</b> The test asserts only that fixtures loaded, both modes ran,
 * results are non-null, and a report was written. Quality numbers exist for humans to read.
 *
 * <p><b>Determinism.</b> Fixtures are static; {@code FakeEmbeddingClient} seeds from
 * {@code text.hashCode()}; RRF ties break on ascending chunk index; Testcontainers gives a
 * fresh DB per run; source ids are derived from a sorted per-case counter.
 *
 * <p><b>No LLM.</b> The pipeline is bypassed — no {@code AnalysisService}, no {@code LlmClient},
 * no Ollama. Only ingestion + retrieval are exercised.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                // Neither Ollama nor OpenAI condition matches "none" → no live embedding bean;
                // the @Primary FakeEmbeddingClient becomes the only EmbeddingClient in scope.
                "app.embedding.provider=none"
        })
@Testcontainers
@Import(RetrievalEvaluationHarnessIT.TestConfig.class)
class RetrievalEvaluationHarnessIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("resumeai")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private DocumentChunkRepository chunkRepository;
    @Autowired
    private EmbeddingClient embeddingClient;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final ObjectMapper mapper = new ObjectMapper();

    // Two retrieval configurations — hybrid stays OFF in prod defaults; the harness
    // constructs its own configs so no environment variable is mutated.
    private final RagConfig vectorOnly = new RagConfig(500, 50, 8, 3500);
    private final RagConfig hybrid = new RagConfig(500, 50, 8, 3500, true, 60, 20);

    private static final int[] K_VALUES = {3, 5, 8};
    private static final String REPORT_HEADER =
            "FOR RELATIVE COMPARISON ONLY — NOT A PRODUCTION GO/NO-GO SIGNAL.";

    @Test
    void evaluateFixturesAndWriteReport() throws Exception {
        List<EvalCase> cases = loadCases();
        assertThat(cases).as("At least one rag-eval fixture must be present").isNotEmpty();

        RetrievalService vectorService = new RetrievalService(chunkRepository, embeddingClient,
                vectorOnly, meterRegistry);
        RetrievalService hybridService = new RetrievalService(chunkRepository, embeddingClient,
                hybrid, meterRegistry);

        // Aggregate reciprocal ranks per (mode, k) so we can average into MRR at the end.
        List<Aggregate> aggregates = new ArrayList<>();
        for (int k : K_VALUES) {
            aggregates.add(new Aggregate("vector", k));
            aggregates.add(new Aggregate("hybrid", k));
        }

        int skipped = 0;
        for (int i = 0; i < cases.size(); i++) {
            EvalCase c = cases.get(i);
            // Deterministic per-case source id — sorted-file-order counter, offset well above
            // anything a real user row might reach in this ephemeral test DB.
            long sourceId = 1_000_000L + i;
            SourceType sourceType = SourceType.valueOf(c.sourceType());
            ingest(c, sourceType, sourceId);

            if (c.relevantChunkIndexes() == null || c.relevantChunkIndexes().isEmpty()) {
                skipped++;
                continue;
            }
            Set<Integer> relevant = new HashSet<>(c.relevantChunkIndexes());

            List<ChunkEvidence> vectorEvidence = vectorService.retrieve(sourceType, sourceId, c.query());
            List<ChunkEvidence> hybridEvidence = hybridService.retrieve(sourceType, sourceId, c.query());
            assertThat(vectorEvidence).as("vector evidence for %s", c.caseId()).isNotNull();
            assertThat(hybridEvidence).as("hybrid evidence for %s", c.caseId()).isNotNull();

            List<Integer> vectorRanked = vectorEvidence.stream().map(ChunkEvidence::chunkIndex).toList();
            List<Integer> hybridRanked = hybridEvidence.stream().map(ChunkEvidence::chunkIndex).toList();

            for (Aggregate a : aggregates) {
                List<Integer> ranked = a.mode.equals("vector") ? vectorRanked : hybridRanked;
                a.recallSum += RetrievalMetrics.recallAtK(relevant, ranked, a.k);
                a.reciprocalRanks.add(RetrievalMetrics.reciprocalRank(relevant, ranked));
                a.evaluatedCases++;
            }
        }

        int evaluated = cases.size() - skipped;
        String report = renderReport(cases.size(), evaluated, skipped, aggregates);
        System.out.println(report);
        Path outPath = Paths.get("target", "rag-eval-report.txt");
        Files.createDirectories(outPath.getParent());
        Files.writeString(outPath, report);

        assertThat(Files.exists(outPath)).as("report file created").isTrue();
    }

    private List<EvalCase> loadCases() throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(getClass().getClassLoader());
        Resource[] resources = resolver.getResources("classpath:rag-eval/cases/*.json");
        // Sort by filename for deterministic ordering across filesystems.
        List<Resource> ordered = new ArrayList<>(Arrays.asList(resources));
        ordered.sort(Comparator.comparing(r -> {
            try {
                return r.getFilename() == null ? "" : r.getFilename();
            } catch (Exception e) {
                return "";
            }
        }));
        List<EvalCase> out = new ArrayList<>();
        for (Resource r : ordered) {
            try (var in = r.getInputStream()) {
                out.add(mapper.readValue(in, EvalCase.class));
            }
        }
        return out;
    }

    private void ingest(EvalCase c, SourceType sourceType, long sourceId) {
        for (EvalCase.Chunk chunk : c.chunks()) {
            float[] embedding = embeddingClient.embed(chunk.content());
            String literal = VectorFormat.toSqlString(embedding);
            chunkRepository.insertChunk(sourceType.name(), sourceId, chunk.index(), chunk.content(), literal);
        }
    }

    private String renderReport(int totalCases, int evaluated, int skipped, List<Aggregate> aggregates) {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================================\n");
        sb.append(" v1.2.M1 Retrieval Evaluation Report\n");
        sb.append(" ").append(REPORT_HEADER).append("\n");
        sb.append("========================================================\n");
        sb.append("cases_total     : ").append(totalCases).append('\n');
        sb.append("cases_evaluated : ").append(evaluated).append('\n');
        sb.append("cases_skipped   : ").append(skipped).append("  (empty relevant set)\n");
        sb.append("embedding       : FakeEmbeddingClient (deterministic; hash-seeded)\n");
        sb.append("rrf_k           : 60\n");
        sb.append("pool_size       : 20 per arm\n");
        sb.append("--------------------------------------------------------\n");
        sb.append(String.format("%-8s %-4s %-12s %-12s%n", "mode", "k", "recall", "mrr"));
        for (Aggregate a : aggregates) {
            double avgRecall = a.evaluatedCases == 0 ? 0.0 : a.recallSum / a.evaluatedCases;
            double mrr = RetrievalMetrics.mrr(a.reciprocalRanks);
            sb.append(String.format("%-8s %-4d %-12.4f %-12.4f%n", a.mode, a.k, avgRecall, mrr));
        }
        sb.append("========================================================\n");
        return sb.toString();
    }

    private static final class Aggregate {
        final String mode;
        final int k;
        double recallSum = 0.0;
        int evaluatedCases = 0;
        final List<Double> reciprocalRanks = new ArrayList<>();

        Aggregate(String mode, int k) {
            this.mode = mode;
            this.k = k;
        }
    }

    /**
     * Test-only config: replaces the (absent, since {@code app.embedding.provider=none}) live
     * embedding client with a deterministic {@link FakeEmbeddingClient}. {@code @Primary} makes
     * this bean win any resolution that might occur in the wider context.
     */
    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        EmbeddingClient fakeEmbeddingClient() {
            return new FakeEmbeddingClient(768);
        }
    }
}
