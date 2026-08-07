package com.princeramteke.resumeai.rag.embedding;

import java.util.List;

/**
 * Provider-agnostic contract for turning text into embedding vectors. The rest of the
 * application codes against this interface only; provider selection (Ollama, OpenAI) is a
 * configuration concern resolved at wiring time. Implementations must return vectors of
 * exactly {@link #dimensions()} length.
 */
public interface EmbeddingClient {

    float[] embed(String text);

    List<float[]> embedBatch(List<String> texts);

    int dimensions();

    String providerName();
}
