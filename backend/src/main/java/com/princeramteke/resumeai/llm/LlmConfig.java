package com.princeramteke.resumeai.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LLM configuration bound from {@code app.llm.*}. {@code provider} selects the active
 * {@link LlmClient} implementation; {@code temperature} and {@code seed} are applied to every
 * request for deterministic scoring (same input → repeatable output, provider permitting).
 * Mirrors {@link com.princeramteke.resumeai.rag.embedding.EmbeddingConfig}.
 */
@ConfigurationProperties(prefix = "app.llm")
public record LlmConfig(
        String provider,
        double temperature,
        Integer seed,
        Ollama ollama,
        OpenAi openai
) {
    public LlmConfig {
        if (provider == null || provider.isBlank()) {
            provider = "ollama";
        }
        if (temperature < 0) {
            temperature = 0.0;
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
                model = "llama3.1:8b";
            }
        }
    }

    public record OpenAi(String baseUrl, String model, String apiKey) {
        public OpenAi {
            if (baseUrl == null || baseUrl.isBlank()) {
                baseUrl = "https://api.openai.com";
            }
            if (model == null || model.isBlank()) {
                model = "gpt-4o-mini";
            }
            if (apiKey == null) {
                apiKey = "";
            }
        }
    }
}
