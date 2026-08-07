package com.princeramteke.resumeai.rag.embedding;

import com.princeramteke.resumeai.rag.exception.EmbeddingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

/**
 * Fallback (cloud) embedding provider. Calls OpenAI's {@code /v1/embeddings} endpoint via a
 * thin {@link RestClient}. Active only when {@code app.embedding.provider} is {@code openai}.
 * OpenAI embeds a batch of inputs in a single request. Sending document text to OpenAI is an
 * opt-in, disclosed choice (see SECURITY.md).
 */
@Component
@ConditionalOnProperty(name = "app.embedding.provider", havingValue = "openai")
public class OpenAiEmbeddingClient implements EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiEmbeddingClient.class);

    private final RestClient restClient;
    private final EmbeddingConfig config;

    public OpenAiEmbeddingClient(RestClient.Builder builder, EmbeddingConfig config) {
        this.config = config;
        this.restClient = builder.baseUrl(config.openai().baseUrl()).build();
    }

    @Override
    public float[] embed(String text) {
        return embedBatch(List.of(text)).get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        log.info("Embedding batch started: provider=openai, count={}", texts.size());
        try {
            OpenAiResponse response = restClient.post()
                    .uri("/v1/embeddings")
                    .header("Authorization", "Bearer " + config.openai().apiKey())
                    .body(new OpenAiRequest(config.openai().model(), texts))
                    .retrieve()
                    .body(OpenAiResponse.class);

            if (response == null || response.data() == null || response.data().isEmpty()) {
                throw new EmbeddingException("OpenAI returned no embeddings");
            }
            List<float[]> result = new ArrayList<>(response.data().size());
            for (OpenAiResponse.Data datum : response.data()) {
                float[] vector = datum.embedding();
                if (vector == null || vector.length != config.dimensions()) {
                    throw new EmbeddingException("Embedding dimension mismatch: expected "
                            + config.dimensions() + " but got " + (vector == null ? 0 : vector.length));
                }
                result.add(vector);
            }
            log.info("Embedding batch finished: provider=openai, count={}", result.size());
            return result;
        } catch (RestClientException e) {
            throw new EmbeddingException("Failed to call OpenAI embedding API", e);
        }
    }

    @Override
    public int dimensions() {
        return config.dimensions();
    }

    @Override
    public String providerName() {
        return "openai";
    }

    record OpenAiRequest(String model, List<String> input) {
    }

    record OpenAiResponse(List<Data> data) {
        record Data(float[] embedding) {
        }
    }
}
