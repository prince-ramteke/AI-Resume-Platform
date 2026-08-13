package com.princeramteke.resumeai.rag.retrieval;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link KeywordQueryBuilder}. All tests are deterministic, no I/O.
 *
 * <p>The builder has two gates: (1) uppercase/digit/non-ASCII heuristic for proper nouns and
 * version numbers, and (2) curated {@code TECH_VOCAB} for common lowercase technology names
 * like {@code kafka}, {@code docker}, {@code python}. Tests cover both gates, stop-word
 * filtering, deduplication, term-limit enforcement, and known adversarial cases.
 */
class KeywordQueryBuilderTest {

    // -------------------------------------------------------------------------
    // Null / blank / empty inputs
    // -------------------------------------------------------------------------

    @Test
    void build_null_returnsEmpty() {
        assertThat(KeywordQueryBuilder.build(null, 5)).isEmpty();
    }

    @Test
    void build_emptyString_returnsEmpty() {
        assertThat(KeywordQueryBuilder.build("", 5)).isEmpty();
    }

    @Test
    void build_blankWhitespace_returnsEmpty() {
        assertThat(KeywordQueryBuilder.build("   ", 5)).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Capitalized technical terms (heuristic gate 1: uppercase letter)
    // -------------------------------------------------------------------------

    @Test
    void build_capitalizedTechnicalTerms_extractsThem() {
        String result = KeywordQueryBuilder.build(
                "Looking for a backend engineer with Spring Boot, PostgreSQL, and Flyway experience.", 10);
        assertThat(result).contains("Spring", "Boot", "PostgreSQL", "Flyway");
        assertThat(result).doesNotContain("Looking", "backend", "engineer", "experience");
    }

    @Test
    void build_allCapsAcronyms_preserved() {
        String result = KeywordQueryBuilder.build("Engineer with AWS JWT REST SQL experience.", 10);
        assertThat(result).contains("AWS", "JWT", "REST", "SQL");
    }

    @Test
    void build_mixedCaseInternalUppercase_preserved() {
        String result = KeywordQueryBuilder.build(
                "Build TypeScript FastAPI gRPC OpenTelemetry services.", 10);
        assertThat(result).contains("TypeScript", "FastAPI", "gRPC", "OpenTelemetry");
    }

    @Test
    void build_versionNumbers_preserved() {
        // "21" contains a digit → kept; "Node" has uppercase → kept.
        String result = KeywordQueryBuilder.build("Java 21 Node 18 service", 10);
        assertThat(result).contains("Java", "21", "Node", "18");
    }

    // -------------------------------------------------------------------------
    // Lowercase technical vocab (gate 2: TECH_VOCAB curated set)
    // -------------------------------------------------------------------------

    @Test
    void build_lowercaseTechVocab_preserved() {
        // kafka, kubernetes, docker, redis are all lowercase and in TECH_VOCAB.
        String result = KeywordQueryBuilder.build(
                "We need python kubernetes docker redis engineers.", 10);
        assertThat(result).contains("python", "kubernetes", "docker", "redis");
    }

    @Test
    void build_mixedCaseAndLowercaseTech_bothPreserved() {
        // TypeScript (uppercase) + terraform (lowercase TECH_VOCAB)
        String result = KeywordQueryBuilder.build("TypeScript terraform microservices pipeline", 10);
        assertThat(result).contains("TypeScript", "terraform", "microservices");
    }

    @Test
    void build_commonLowercaseTechnologies_preserved() {
        String result = KeywordQueryBuilder.build(
                "looking for kubernetes docker terraform kafka experience", 10);
        assertThat(result).contains("kubernetes", "docker", "terraform", "kafka");
        assertThat(result).doesNotContain("looking", "experience");
    }

    // -------------------------------------------------------------------------
    // Prose / stop-word filtering
    // -------------------------------------------------------------------------

    @Test
    void build_genericProse_returnsEmpty() {
        // All stop words or all-lowercase non-tech tokens.
        String result = KeywordQueryBuilder.build(
                "looking for a great developer to work with our collaborative team", 10);
        assertThat(result).isEmpty();
    }

    @Test
    void build_semanticOnlyProse_returnsEmpty() {
        // "technical", "lead", "cross-team", "architectural", "decisions", "improvements"
        // are all lowercase non-TECH_VOCAB tokens.
        String result = KeywordQueryBuilder.build(
                "We need a technical lead who drives cross-team architectural decisions and team improvements", 5);
        assertThat(result).isEmpty();
    }

    @Test
    void build_adversarialCachingProse_returnsEmpty() {
        // "production" is a stop word; "caching", "experience" lowercase non-vocab.
        // This mirrors fixture 15: adversarial case where keyword arm should be skipped.
        String result = KeywordQueryBuilder.build(
                "Engineer with hands-on production caching experience", 5);
        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Mixed JD text
    // -------------------------------------------------------------------------

    @Test
    void build_mixedProseAndTechnical_extractsOnlyTechnical() {
        String result = KeywordQueryBuilder.build(
                "looking for a senior backend engineer with strong kubernetes and PostgreSQL experience to join our team", 5);
        assertThat(result).contains("kubernetes", "PostgreSQL");
        assertThat(result).doesNotContain("looking", "senior", "strong", "experience");
    }

    @Test
    void build_jdSentence_kotlin_lowercaseInVocab() {
        String result = KeywordQueryBuilder.build(
                "Backend engineer experienced in kotlin and grpc for microservices work", 10);
        assertThat(result).contains("kotlin", "grpc", "microservices");
        assertThat(result).doesNotContain("Backend", "engineer", "experienced", "work");
    }

    // -------------------------------------------------------------------------
    // Deduplication (order-preserving)
    // -------------------------------------------------------------------------

    @Test
    void build_duplicateTerms_firstOccurrenceKeptOnce() {
        String result = KeywordQueryBuilder.build("Spring Boot Spring PostgreSQL Spring", 10);
        long springCount = Arrays.stream(result.split("\\s+"))
                .filter("Spring"::equals).count();
        assertThat(springCount).isEqualTo(1);
        assertThat(result).contains("Spring", "Boot", "PostgreSQL");
    }

    // -------------------------------------------------------------------------
    // Term-limit enforcement
    // -------------------------------------------------------------------------

    @Test
    void build_maxTermsBounded_stopsAfterLimit() {
        String result = KeywordQueryBuilder.build(
                "Java Spring Boot PostgreSQL Docker Redis Kafka Kubernetes", 3);
        String[] parts = result.split("\\s+");
        assertThat(parts).hasSize(3);
        assertThat(parts[0]).isEqualTo("Java");
        assertThat(parts[1]).isEqualTo("Spring");
        assertThat(parts[2]).isEqualTo("Boot");
    }

    @Test
    void build_maxTermsOne_returnsSingleTerm() {
        String result = KeywordQueryBuilder.build("Java Spring Boot Kubernetes", 1);
        assertThat(result).isEqualTo("Java");
    }

    @Test
    void build_maxTermsZeroOrNegative_treatedAsOne() {
        // Clamped to Math.max(1, maxTerms) — at least 1 term is always extracted.
        String result = KeywordQueryBuilder.build("Spring Boot", 0);
        assertThat(result).isEqualTo("Spring");

        String result2 = KeywordQueryBuilder.build("Spring Boot", -5);
        assertThat(result2).isEqualTo("Spring");
    }

    // -------------------------------------------------------------------------
    // Specific fixture-query traces (verify builder output for eval harness)
    // -------------------------------------------------------------------------

    @Test
    void build_fixture01_springBootPostgresqlFlyway() {
        // Fixture 01: realistic JD sentence for the Spring Boot / Flyway case
        String result = KeywordQueryBuilder.build(
                "Looking for a backend engineer with Spring Boot, PostgreSQL, and Flyway experience.", 5);
        assertThat(result).isEqualTo("Spring Boot PostgreSQL Flyway");
    }

    @Test
    void build_fixture03_kafka() {
        // Fixture 03: single Kafka keyword JD
        String result = KeywordQueryBuilder.build(
                "Hiring a senior Kafka engineer for our high-throughput event processing platform.", 5);
        assertThat(result).isEqualTo("Kafka");
    }

    @Test
    void build_fixture07_java21ProjectLoom() {
        // Fixture 07: "Java 21" and "Project Loom" virtual threads
        String result = KeywordQueryBuilder.build(
                "Backend engineer for a Java 21 service with Project Loom virtual threads and high-throughput HTTP handling.", 5);
        // "Project" → lowercase "project" is a stop word → FILTERED.
        // "Loom" (uppercase L), "21" (digit), "HTTP" (uppercase) all pass.
        // Result: "Java 21 Loom HTTP" — FTS 'java' & '21' & 'loom' & 'http' hits chunk 0.
        assertThat(result).isEqualTo("Java 21 Loom HTTP");
        assertThat(result).doesNotContain("Project"); // 'project' is a stop word
    }

    @Test
    void build_fixture14_pythonFastapiMicroservicesGrpc() {
        // Fixture 14: Python + FastAPI + microservices (TECH_VOCAB) + gRPC
        String result = KeywordQueryBuilder.build(
                "Backend Python engineer to build FastAPI microservices with gRPC inter-service communication.", 5);
        assertThat(result).contains("Python", "FastAPI", "microservices", "gRPC");
    }

    @Test
    void build_fixture15_adversarialCachingJd_returnsEmpty() {
        // Fixture 15: adversarial — JD has "production caching" but no distinctive tech terms
        String result = KeywordQueryBuilder.build(
                "Engineer with hands-on production caching experience using in-process and distributed cache strategies.", 5);
        assertThat(result).isEmpty();
    }
}
