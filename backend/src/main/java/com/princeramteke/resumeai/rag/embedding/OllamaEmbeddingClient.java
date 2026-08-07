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
 * Default (local) embedding provider. Calls Ollama's {@code /api/embeddings} endpoint via a
 * thin {@link RestClient}. Active when {@code app.embedding.provider} is {@code ollama} or
 * unset. Ollama embeds one prompt per request, so batching loops over the single call.
 */
@Component
@ConditionalOnProperty(name = "app.embedding.provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaEmbeddingClient implements EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingClient.class);

    private final RestClient restClient;
    private final EmbeddingConfig config;

    public OllamaEmbeddingClient(RestClient.Builder builder, EmbeddingConfig config) {
        this.config = config;
        this.restClient = builder.baseUrl(config.ollama().baseUrl()).build();
    }

    @Override
    public float[] embed(String text) {
        try {
            OllamaResponse response = restClient.post()
                    .uri("/api/embeddings")
                    .body(new OllamaRequest(config.ollama().model(), text))
                    .retrieve()
                    .body(OllamaResponse.class);

            if (response == null || response.embedding() == null) {
                throw new EmbeddingException("Ollama returned an empty embedding");
            }
            float[] vector = response.embedding();
            if (vector.length != config.dimensions()) {
                throw new EmbeddingException("Embedding dimension mismatch: expected "
                        + config.dimensions() + " but got " + vector.length);
            }
            return vector;
        } catch (RestClientException e) {
            throw new EmbeddingException("Failed to call Ollama embedding API", e);
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        log.info("Embedding batch started: provider=ollama, count={}", texts.size());
        List<float[]> result = new ArrayList<>(texts.size());
        for (String text : texts) {
            result.add(embed(text));
        }
        log.info("Embedding batch finished: provider=ollama, count={}", result.size());
        return result;
    }

    @Override
    public int dimensions() {
        return config.dimensions();
    }

    @Override
    public String providerName() {
        return "ollama";
    }

    record OllamaRequest(String model, String prompt) {
    }

    record OllamaResponse(float[] embedding) {
    }
}
