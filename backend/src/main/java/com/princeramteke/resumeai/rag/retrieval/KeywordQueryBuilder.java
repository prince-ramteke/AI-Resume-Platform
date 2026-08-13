package com.princeramteke.resumeai.rag.retrieval;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Deterministic, no-dependency utility that converts arbitrary query text (e.g., a full job
 * description) into a short, bounded keyword string suitable for PostgreSQL FTS via
 * {@code plainto_tsquery}. Solves the production problem where passing a full JD sentence
 * to {@code plainto_tsquery} generates an AND-conjunction of 15–40 stems that no single
 * resume chunk can satisfy, causing the keyword arm of hybrid retrieval to return 0 candidates.
 *
 * <h3>Algorithm (O(n) on input length, no I/O, no Spring dependency)</h3>
 * <ol>
 *   <li>Split on whitespace and common punctuation.</li>
 *   <li>Discard stop words (English function words + common JD prose terms).</li>
 *   <li>Keep a token if it satisfies <em>either</em>:
 *     <ul>
 *       <li>Contains an uppercase letter, a digit, or a non-ASCII character
 *           (captures "Java", "Spring", "JWT", "K8s", "TypeScript", "21").</li>
 *       <li>Its lowercase form is in the curated {@code TECH_VOCAB} list
 *           (captures "kafka", "kubernetes", "docker", "redis" — common tech names
 *           written lowercase in JDs).</li>
 *     </ul>
 *   </li>
 *   <li>De-duplicate by first occurrence (order-preserving via {@link LinkedHashSet}).</li>
 *   <li>Stop when {@code maxTerms} is reached.</li>
 *   <li>Join with space; the caller passes the result to {@code plainto_tsquery}.</li>
 * </ol>
 *
 * <p><b>Security:</b> the result is passed as a bind parameter to a parameterized JPA query
 * — no SQL injection risk. No user identity or query text appears in metric tags.
 */
public final class KeywordQueryBuilder {

    private KeywordQueryBuilder() {}

    // -------------------------------------------------------------------------
    // Stop-word list: English function words + JD-specific prose terms that
    // appear as false positives (often capitalised at sentence start).
    // Compared case-insensitively against each token's lowercase form.
    // -------------------------------------------------------------------------
    private static final Set<String> STOP_WORDS = Set.of(
            // English function words
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of",
            "with", "by", "from", "as", "it", "its", "we", "our", "you", "your", "they",
            "their", "who", "which", "when", "where", "how", "what", "this", "that",
            "these", "those", "not", "if", "so", "up", "out", "no", "be", "been",
            "being", "are", "is", "was", "has", "have", "had", "will", "would", "can",
            "could", "should", "may", "might", "must", "shall", "do", "does", "did",
            "all", "each", "any", "some", "more", "most", "other", "also", "such",
            "very", "just", "about", "into", "through", "while", "after", "before",
            "over", "under", "within", "without", "between", "among", "per", "etc",
            "via", "vs", "re",
            // JD-prose terms (including typical sentence-start capitalisations)
            "looking", "seeking", "hiring", "join", "joining", "help", "work",
            "working", "build", "building", "develop", "developing", "design",
            "designing", "lead", "leading", "manage", "managing", "create", "creating",
            "maintain", "maintaining", "implement", "implementing",
            // Generic role/skill nouns
            "experience", "experienced", "background", "knowledge", "skills", "skill",
            "ability", "familiar", "comfortable", "understanding", "passion", "passionate",
            "strong", "excellent", "solid", "deep", "proven", "demonstrated", "extensive",
            "engineer", "engineers", "developer", "developers", "programmer", "senior",
            "junior", "role", "position", "job", "candidate", "ideal", "perfect",
            "great", "good",
            // Generic system/team/product nouns
            "team", "teams", "company", "startup", "product", "platform", "service",
            "services", "system", "systems", "stack", "codebase", "code", "project",
            "projects", "environment", "production", "deployment", "infrastructure",
            "architecture", "responsible", "responsibilities", "require", "requirements",
            "required", "opportunity", "modern", "latest", "innovative", "collaborative",
            // Time/scale qualifiers
            "years", "year", "months", "month", "minimum", "plus", "ideally",
            "preferred", "key", "primary", "main", "major", "core", "critical",
            "essential", "important", "big", "high", "low", "large", "small", "real",
            "current", "existing", "previous",
            // Misc JD filler
            "using", "used", "use", "new", "both", "based", "scale", "scaling",
            "fast", "growing", "hands", "end", "side", "full", "back", "front",
            "need", "own", "well", "see", "get", "set", "run", "write", "wrote",
            "know", "make", "built", "made", "took", "take", "put", "come",
            "backend", "frontend", "fullstack"
    );

    // -------------------------------------------------------------------------
    // Technical vocabulary: common technology names that appear lowercase in JDs
    // but would be filtered by the uppercase/digit heuristic. Kept intentionally
    // small (~100 entries) — covers the most frequent terms in the resume-AI domain.
    // -------------------------------------------------------------------------
    static final Set<String> TECH_VOCAB = Set.of(
            // Languages
            "java", "python", "kotlin", "scala", "golang", "ruby", "rust", "swift",
            "php", "perl", "typescript", "javascript", "bash", "shell", "groovy",
            "clojure", "elixir", "haskell", "ocaml", "lua", "dart", "zig",
            // Frameworks / libraries
            "spring", "react", "angular", "vue", "django", "flask", "rails", "express",
            "fastapi", "hibernate", "quarkus", "micronaut", "nextjs", "nuxt", "svelte",
            "laravel", "symfony", "gin", "echo", "fiber",
            // Databases
            "postgresql", "postgres", "mysql", "oracle", "sqlite", "mongodb", "cassandra",
            "elasticsearch", "redis", "memcached", "dynamodb", "mariadb", "neo4j",
            "influxdb", "clickhouse", "cockroachdb", "firestore", "couchdb",
            // Infrastructure / platform
            "kubernetes", "docker", "terraform", "ansible", "helm", "istio", "envoy",
            "nginx", "linux", "unix", "debian", "ubuntu", "centos", "alpine",
            "vagrant", "packer", "consul", "vault",
            // Cloud
            "aws", "azure", "gcp", "heroku", "eks", "aks", "gke", "ec2", "s3",
            "rds", "lambda", "sqs", "sns", "kinesis", "cloudwatch", "cloudfront",
            "fargate", "ecs", "ecr",
            // Build / VCS
            "maven", "gradle", "npm", "yarn", "pip", "cargo", "bazel", "git",
            "github", "gitlab", "bitbucket", "jenkins", "circleci", "argocd",
            "tekton", "concourse",
            // Protocols / standards
            "jwt", "oauth", "saml", "ldap", "grpc", "graphql", "amqp", "websocket",
            "protobuf", "avro", "thrift", "openapi", "swagger",
            // Messaging
            "kafka", "rabbitmq", "activemq", "nats", "pulsar", "celery", "sidekiq",
            // Observability
            "opentelemetry", "micrometer", "zipkin", "jaeger", "sentry", "newrelic",
            "prometheus", "grafana", "datadog", "splunk", "kibana", "dynatrace",
            // Concepts / paradigms (lowercase in JDs)
            "microservices", "serverless", "devops", "mlops", "gitops", "sre",
            "cicd", "tdd", "bdd", "ddd", "cqrs", "iac"
    );

    /**
     * Extracts up to {@code maxTerms} distinctive technical terms from {@code text}.
     *
     * @param text      arbitrary query text (e.g., a full job description); {@code null} is safe
     * @param maxTerms  upper bound on the number of returned terms; clamped to ≥ 1
     * @return space-separated technical keyword string, or {@code ""} if nothing qualifies
     */
    public static String build(String text, int maxTerms) {
        if (text == null || text.isBlank()) return "";
        int limit = Math.max(1, maxTerms);

        // Split on whitespace and common punctuation; hyphens are NOT separators
        // so "K8s", "CI/CD" → ["K8s", "CI", "CD"] is handled correctly.
        String[] rawTokens = text.split("[\\s,;:.()•\\[\\]{}'\"!?/\\\\|+*@#$%^&=>~`]+");

        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String raw : rawTokens) {
            String token = raw.trim();
            if (token.length() < 2) continue;

            String lower = token.toLowerCase(Locale.ROOT);
            if (STOP_WORDS.contains(lower)) continue;

            // Gate 1: contains uppercase letter, digit, or non-ASCII (heuristic for proper
            //         nouns, acronyms, version numbers: "Java", "JWT", "21", "K8s", "gRPC")
            // Gate 2: normalised form is in the curated tech vocabulary ("kafka", "docker")
            boolean keep = TECH_VOCAB.contains(lower);
            if (!keep) {
                for (int i = 0; i < token.length(); i++) {
                    char c = token.charAt(i);
                    if (Character.isUpperCase(c) || Character.isDigit(c) || c > 127) {
                        keep = true;
                        break;
                    }
                }
            }
            if (!keep) continue;

            seen.add(token);
            if (seen.size() >= limit) break;
        }

        return String.join(" ", seen);
    }
}
