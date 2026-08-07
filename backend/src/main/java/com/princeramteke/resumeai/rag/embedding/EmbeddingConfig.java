package com.princeramteke.resumeai.rag.embedding;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Embedding configuration bound from {@code app.embedding.*}. {@code provider} selects the
 * active {@link EmbeddingClient} implementation; {@code dimensions} is the single source of
 * truth for the vector length and must match both the chosen model and the {@code vector(N)}
 * column in the schema.
 */
@ConfigurationProperties(prefix = "app.embedding")
public record EmbeddingConfig(
        String provider,
        int dimensions,
        Ollama ollama,
        OpenAi openai
) {
    public EmbeddingConfig {
        if (provider == null || provider.isBlank()) {
            provider = "ollama";
        }
        if (dimensions <= 0) {
            dimensions = 768;
        }
        if (ollama == null) {
            ollama = new Ollama(null, null);
        }
        if (openai == null) {
            openai = new OpenAi(null, null, null);
        }
    }

    public record Ollama(String baseUrl, String model) {
        public Ollama {
            if (baseUrl == null || baseUrl.isBlank()) {
                baseUrl = "http://localhost:11434";
            }
            if (model == null || model.isBlank()) {
                model = "nomic-embed-text";
            }
        }
    }

    public record OpenAi(String baseUrl, String model, String apiKey) {
        public OpenAi {
            if (baseUrl == null || baseUrl.isBlank()) {
                baseUrl = "https://api.openai.com";
            }
            if (model == null || model.isBlank()) {
                model = "text-embedding-3-small";
            }
            if (apiKey == null) {
                apiKey = "";
            }
        }
    }
}
