package com.princeramteke.resumeai.llm;

/**
 * Provider-agnostic contract for a single-shot chat completion. The rest of the application
 * codes against this interface only; provider selection (Ollama, OpenAI) is a configuration
 * concern resolved at wiring time via {@code app.llm.provider}. Mirrors the
 * {@link com.princeramteke.resumeai.rag.embedding.EmbeddingClient} design so that swapping
 * models is a configuration change, not a code change.
 */
public interface LlmClient {

    /** Send one system + user prompt and return the model's completion plus token usage. */
    LlmResponse complete(LlmRequest request);

    /** Stable provider identifier persisted on each analysis (e.g. {@code "ollama"}). */
    String providerName();
}
