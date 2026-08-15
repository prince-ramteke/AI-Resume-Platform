package com.princeramteke.resumeai.rag.embedding;

import com.fasterxml.jackson.annotation.JsonProperty;
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
 * Gemini embedding provider. Calls {@code batchEmbedContents} on the Gemini generative language
 * API. Active only when {@code app.embedding.provider} is {@code gemini}. Requests
 * {@code outputDimensionality} from {@code app.embedding.dimensions} — the single source of truth
 * for the vector length shared with the schema column.
 * Authentication uses the {@code x-goog-api-key} header (not Bearer). Input ordering is
 * preserved: the response {@code embeddings[]} array is index-aligned with the request
 * {@code requests[]} array per the Gemini API contract.
 */
@Component
@ConditionalOnProperty(name = "app.embedding.provider", havingValue = "gemini")
public class GeminiEmbeddingClient implements EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiEmbeddingClient.class);
    private static final String BATCH_PATH = "/v1beta/models/{model}:batchEmbedContents";

    private final RestClient restClient;
    private final EmbeddingConfig config;

    public GeminiEmbeddingClient(RestClient.Builder builder, EmbeddingConfig config) {
        this.config = config;
        this.restClient = builder.baseUrl(config.gemini().baseUrl()).build();
    }

    @Override
    public float[] embed(String text) {
        return embedBatch(List.of(text)).get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        log.info("Embedding batch started: provider=gemini, count={}", texts.size());
        String model = config.gemini().model();
        try {
            // Gemini batchEmbedContents requires the fully-qualified resource name
            // ("models/<name>") in every request-body EmbedRequest, even though the
            // URL path already encodes the bare model name via {model} substitution.
            String qualifiedModel = "models/" + model;
            List<BatchRequest.EmbedRequest> requests = texts.stream()
                    .map(t -> new BatchRequest.EmbedRequest(
                            qualifiedModel,
                            new BatchRequest.Content(List.of(new BatchRequest.Part(t))),
                            new BatchRequest.EmbedConfig(config.dimensions())))
                    .toList();

            BatchResponse response = restClient.post()
                    .uri(BATCH_PATH, model)
                    .header("x-goog-api-key", config.gemini().apiKey())
                    .body(new BatchRequest(requests))
                    .retrieve()
                    .body(BatchResponse.class);

            if (response == null || response.embeddings() == null || response.embeddings().isEmpty()) {
                throw new EmbeddingException("Gemini returned no embeddings");
            }
            if (response.embeddings().size() != texts.size()) {
                throw new EmbeddingException("Gemini returned " + response.embeddings().size()
                        + " embeddings for " + texts.size() + " inputs");
            }

            List<float[]> result = new ArrayList<>(response.embeddings().size());
            for (BatchResponse.Embedding embedding : response.embeddings()) {
                float[] vector = embedding.values();
                if (vector == null || vector.length != config.dimensions()) {
                    throw new EmbeddingException("Embedding dimension mismatch: expected "
                            + config.dimensions() + " but got " + (vector == null ? 0 : vector.length));
                }
                result.add(vector);
            }
            log.info("Embedding batch finished: provider=gemini, count={}", result.size());
            return result;
        } catch (RestClientException e) {
            throw new EmbeddingException("Failed to call Gemini embedding API", e);
        }
    }

    @Override
    public int dimensions() {
        return config.dimensions();
    }

    @Override
    public String providerName() {
        return "gemini";
    }

    record BatchRequest(List<EmbedRequest> requests) {
        record EmbedRequest(String model, Content content,
                            @JsonProperty("embedContentConfig") EmbedConfig embedContentConfig) {
        }

        record Content(List<Part> parts) {
        }

        record Part(String text) {
        }

        record EmbedConfig(@JsonProperty("outputDimensionality") int outputDimensionality) {
        }
    }

    record BatchResponse(List<Embedding> embeddings) {
        record Embedding(float[] values) {
        }
    }
}
