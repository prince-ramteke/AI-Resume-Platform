package com.princeramteke.resumeai.rag.embedding;

import com.princeramteke.resumeai.rag.exception.EmbeddingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiEmbeddingClientTest {

    private static final String BASE = "https://generativelanguage.googleapis.com";
    private static final String MODEL = "gemini-embedding-2";
    private static final String BATCH_URL =
            BASE + "/v1beta/models/" + MODEL + ":batchEmbedContents";

    private MockRestServiceServer server;
    private GeminiEmbeddingClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        var config = new EmbeddingConfig("gemini", 3, null, null,
                new EmbeddingConfig.Gemini(BASE, MODEL, "test-gemini-key"));
        client = new GeminiEmbeddingClient(builder, config);
    }

    // ── batch request construction + response parsing ─────────────────────

    @Test
    void embedBatch_sendsApiKeyHeaderAndReturnsVectorsInOrder() {
        server.expect(requestTo(BATCH_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-gemini-key"))
                .andExpect(content().json(
                        "{\"requests\":[" +
                        "{\"embedContentConfig\":{\"outputDimensionality\":3}}," +
                        "{\"embedContentConfig\":{\"outputDimensionality\":3}}" +
                        "]}",
                        false))
                .andRespond(withSuccess(
                        """
                        {"embeddings":[
                          {"values":[0.1,0.2,0.3]},
                          {"values":[0.4,0.5,0.6]}
                        ]}
                        """,
                        MediaType.APPLICATION_JSON));

        List<float[]> result = client.embedBatch(List.of("first", "second"));

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsExactly(0.1f, 0.2f, 0.3f);
        assertThat(result.get(1)).containsExactly(0.4f, 0.5f, 0.6f);
        server.verify();
    }

    @Test
    void embed_singleText_returnsFirstVector() {
        server.expect(requestTo(BATCH_URL))
                .andRespond(withSuccess(
                        "{\"embeddings\":[{\"values\":[0.7,0.8,0.9]}]}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.embed("hello")).containsExactly(0.7f, 0.8f, 0.9f);
    }

    // ── ordering preservation ─────────────────────────────────────────────

    @Test
    void embedBatch_preservesInputOrdering() {
        server.expect(requestTo(BATCH_URL))
                .andRespond(withSuccess(
                        """
                        {"embeddings":[
                          {"values":[1.0,0.0,0.0]},
                          {"values":[0.0,1.0,0.0]},
                          {"values":[0.0,0.0,1.0]}
                        ]}
                        """,
                        MediaType.APPLICATION_JSON));

        List<float[]> result = client.embedBatch(List.of("a", "b", "c"));

        assertThat(result.get(0)).containsExactly(1.0f, 0.0f, 0.0f);
        assertThat(result.get(1)).containsExactly(0.0f, 1.0f, 0.0f);
        assertThat(result.get(2)).containsExactly(0.0f, 0.0f, 1.0f);
    }

    // ── 1536-dimensional validation ───────────────────────────────────────

    @Test
    void embedBatch_1536Dimensions_validatesCorrectly() {
        RestClient.Builder builder1536 = RestClient.builder();
        MockRestServiceServer server1536 = MockRestServiceServer.bindTo(builder1536).build();
        var config1536 = new EmbeddingConfig("gemini", 1536, null, null,
                new EmbeddingConfig.Gemini(BASE, MODEL, "key"));
        GeminiEmbeddingClient client1536 = new GeminiEmbeddingClient(builder1536, config1536);

        float[] vec = new float[1536];
        for (int i = 0; i < 1536; i++) vec[i] = i * 0.001f;
        String valuesJson = buildValuesJson(vec);

        server1536.expect(requestTo(BATCH_URL))
                .andRespond(withSuccess(
                        "{\"embeddings\":[{\"values\":" + valuesJson + "}]}",
                        MediaType.APPLICATION_JSON));

        float[] result = client1536.embed("resume text");

        assertThat(result).hasSize(1536);
        assertThat(client1536.dimensions()).isEqualTo(1536);
        server1536.verify();
    }

    @Test
    void embedBatch_dimensionMismatch_throwsEmbeddingException() {
        server.expect(requestTo(BATCH_URL))
                .andRespond(withSuccess(
                        "{\"embeddings\":[{\"values\":[0.1,0.2]}]}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.embed("hello"))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("dimension mismatch");
    }

    @Test
    void embedBatch_countMismatch_throwsEmbeddingException() {
        server.expect(requestTo(BATCH_URL))
                .andRespond(withSuccess(
                        "{\"embeddings\":[{\"values\":[0.1,0.2,0.3]}]}",
                        MediaType.APPLICATION_JSON));

        // sent 2 texts but server returned 1 embedding
        assertThatThrownBy(() -> client.embedBatch(List.of("a", "b")))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("1 embeddings for 2 inputs");
    }

    // ── API failure handling ──────────────────────────────────────────────

    @Test
    void embedBatch_providerError_throwsEmbeddingException() {
        server.expect(requestTo(BATCH_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.embed("hello"))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("Gemini");
    }

    @Test
    void embedBatch_emptyEmbeddings_throwsEmbeddingException() {
        server.expect(requestTo(BATCH_URL))
                .andRespond(withSuccess("{\"embeddings\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.embed("hello"))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("no embeddings");
    }

    // ── provider identity ─────────────────────────────────────────────────

    @Test
    void providerName_isGemini() {
        assertThat(client.providerName()).isEqualTo("gemini");
    }

    @Test
    void dimensions_returnsConfiguredValue() {
        assertThat(client.dimensions()).isEqualTo(3);
    }

    // ── helper ────────────────────────────────────────────────────────────

    private static String buildValuesJson(float[] vec) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vec[i]);
        }
        return sb.append(']').toString();
    }
}
