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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * v1.2.M2 RRF tuning sweep. Evaluation-only, deterministic, no LLM.
 *
 * <p>Runs the hybrid retrieval path (only) over the same fixture set used by
 * {@link RetrievalEvaluationHarnessIT}, across the Cartesian product of
 * {@code rrfK ∈ {30, 60, 90}} and {@code candidatePoolSize ∈ {10, 20, 40}} — nine combinations.
 * Records Recall@{3,5,8} and MRR per combination and writes
 * {@code target/rag-eval-tuning-report.txt} plus stdout.
 *
 * <p><b>Production defaults are not touched.</b> Each combination is constructed via a local
 * {@link RagConfig} instance passed to a fresh {@link RetrievalService}; {@code application.yml}
 * remains {@code hybrid-enabled=false, rrf_k=60, pool=20}. This test is a measurement tool.
 *
 * <p><b>Structural assertions only.</b> The sweep does not fail on any quality threshold — the
 * humans reviewing {@code docs/eval/retrieval-report.md} make the decision.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "app.embedding.provider=none"
        })
@Testcontainers
@Import(RetrievalTuningIT.TestConfig.class)
class RetrievalTuningIT {

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

    private final ObjectMapper mapper = new ObjectMapper();
    private static final int[] K_VALUES = {3, 5, 8};
    private static final int[] RRF_K_VALUES = {30, 60, 90};
    private static final int[] POOL_VALUES = {10, 20, 40};

    @Test
    void rrfAndPoolSweepAndWriteReport() throws Exception {
        List<EvalCase> cases = loadCases();
        assertThat(cases).as("fixtures must exist").isNotEmpty();

        // Ingest each case exactly once — the fixture DB is shared across sweep combinations.
        for (int i = 0; i < cases.size(); i++) {
            EvalCase c = cases.get(i);
            ingest(c, SourceType.valueOf(c.sourceType()), 1_000_000L + i);
        }

        List<SweepRow> rows = new ArrayList<>();
        for (int rrfK : RRF_K_VALUES) {
            for (int pool : POOL_VALUES) {
                rows.add(runSweep(cases, rrfK, pool));
            }
        }

        String report = renderReport(cases.size(), rows);
        System.out.println(report);
        Path outPath = Paths.get("target", "rag-eval-tuning-report.txt");
        Files.createDirectories(outPath.getParent());
        Files.writeString(outPath, report);

        assertThat(Files.exists(outPath)).isTrue();
    }

    private SweepRow runSweep(List<EvalCase> cases, int rrfK, int pool) {
        // Each combination uses a fresh meter registry so cross-combination metrics don't
        // pollute one another — we only care about quality numbers here, not observability.
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RagConfig cfg = new RagConfig(500, 50, 8, 3500, true, rrfK, pool);
        RetrievalService service = new RetrievalService(chunkRepository, embeddingClient, cfg, registry);

        double r3 = 0.0, r5 = 0.0, r8 = 0.0;
        List<Double> reciprocalRanks = new ArrayList<>();
        int evaluated = 0;

        for (int i = 0; i < cases.size(); i++) {
            EvalCase c = cases.get(i);
            if (c.relevantChunkIndexes() == null || c.relevantChunkIndexes().isEmpty()) {
                continue;
            }
            Set<Integer> relevant = new HashSet<>(c.relevantChunkIndexes());
            long sourceId = 1_000_000L + i;
            List<ChunkEvidence> evidence = service.retrieve(
                    SourceType.valueOf(c.sourceType()), sourceId, c.query());
            List<Integer> ranked = evidence.stream().map(ChunkEvidence::chunkIndex).toList();

            r3 += RetrievalMetrics.recallAtK(relevant, ranked, 3);
            r5 += RetrievalMetrics.recallAtK(relevant, ranked, 5);
            r8 += RetrievalMetrics.recallAtK(relevant, ranked, 8);
            reciprocalRanks.add(RetrievalMetrics.reciprocalRank(relevant, ranked));
            evaluated++;
        }

        double n = evaluated == 0 ? 1 : evaluated;
        return new SweepRow(rrfK, pool, r3 / n, r5 / n, r8 / n, RetrievalMetrics.mrr(reciprocalRanks), evaluated);
    }

    private List<EvalCase> loadCases() throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(getClass().getClassLoader());
        Resource[] resources = resolver.getResources("classpath:rag-eval/cases/*.json");
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

    private String renderReport(int fixtureCount, List<SweepRow> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================================\n");
        sb.append(" v1.2.M2 RRF Tuning Sweep (hybrid retrieval only)\n");
        sb.append(" FOR RELATIVE COMPARISON ONLY — NOT A PRODUCTION GO/NO-GO SIGNAL.\n");
        sb.append("========================================================\n");
        sb.append("run_date        : ").append(OffsetDateTime.now()).append('\n');
        sb.append("fixture_count   : ").append(fixtureCount).append('\n');
        sb.append("embedder        : FakeEmbeddingClient (768-dim, hash-seeded, deterministic)\n");
        sb.append("k_values        : ").append(Arrays.toString(K_VALUES)).append('\n');
        sb.append("rrf_k_swept     : ").append(Arrays.toString(RRF_K_VALUES)).append('\n');
        sb.append("pool_swept      : ").append(Arrays.toString(POOL_VALUES)).append('\n');
        sb.append("--------------------------------------------------------\n");
        sb.append(String.format("%-6s %-6s %-10s %-10s %-10s %-10s %-6s%n",
                "rrf_k", "pool", "R@3", "R@5", "R@8", "MRR", "cases"));
        for (SweepRow r : rows) {
            String marker = (r.rrfK == 60 && r.pool == 20) ? "  <- prod default" : "";
            sb.append(String.format("%-6d %-6d %-10.4f %-10.4f %-10.4f %-10.4f %-6d%s%n",
                    r.rrfK, r.pool, r.r3, r.r5, r.r8, r.mrr, r.evaluated, marker));
        }
        sb.append("========================================================\n");
        return sb.toString();
    }

    private record SweepRow(int rrfK, int pool, double r3, double r5, double r8, double mrr, int evaluated) {
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        EmbeddingClient fakeEmbeddingClient() {
            return new FakeEmbeddingClient(768);
        }
    }
}
