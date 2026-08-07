package com.princeramteke.resumeai.rag.embedding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Deterministic test double for {@link EmbeddingClient}: derives a stable pseudo-random
 * vector from the text's hash, so the same text always embeds to the same vector and no
 * network is touched. Mirrors the pattern in docs/TESTING.md.
 */
public class FakeEmbeddingClient implements EmbeddingClient {

    private final int dimensions;

    public FakeEmbeddingClient() {
        this(768);
    }

    public FakeEmbeddingClient(int dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[dimensions];
        Random random = new Random(text == null ? 0 : text.hashCode());
        for (int i = 0; i < dimensions; i++) {
            vector[i] = random.nextFloat();
        }
        return vector;
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> result = new ArrayList<>(texts.size());
        for (String text : texts) {
            result.add(embed(text));
        }
        return result;
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    @Override
    public String providerName() {
        return "fake";
    }
}
