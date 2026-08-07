package com.princeramteke.resumeai.rag.embedding;

import com.princeramteke.resumeai.rag.exception.EmbeddingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OllamaEmbeddingClientTest {

    private static final String URL = "http://localhost:11434/api/embeddings";

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private OllamaEmbeddingClient client;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        var config = new EmbeddingConfig("ollama", 3,
                new EmbeddingConfig.Ollama("http://localhost:11434", "nomic-embed-text"), null);
        client = new OllamaEmbeddingClient(builder, config);
    }

    @Test
    void embed_validResponse_returnsVector() {
        server.expect(requestTo(URL))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"embedding\":[0.1,0.2,0.3]}", MediaType.APPLICATION_JSON));

        float[] vector = client.embed("hello");

        assertThat(vector).containsExactly(0.1f, 0.2f, 0.3f);
        server.verify();
    }

    @Test
    void embed_dimensionMismatch_throwsEmbeddingException() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("{\"embedding\":[0.1,0.2]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.embed("hello"))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("dimension mismatch");
    }

    @Test
    void embed_providerError_throwsEmbeddingException() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.embed("hello"))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("Ollama");
    }

    @Test
    void embedBatch_embedsEachText() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("{\"embedding\":[0.1,0.2,0.3]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(URL))
                .andRespond(withSuccess("{\"embedding\":[0.4,0.5,0.6]}", MediaType.APPLICATION_JSON));

        var result = client.embedBatch(java.util.List.of("a", "b"));

        assertThat(result).hasSize(2);
        assertThat(result.get(1)).containsExactly(0.4f, 0.5f, 0.6f);
        server.verify();
    }

    @Test
    void dimensionsAndProviderName_areExposed() {
        assertThat(client.dimensions()).isEqualTo(3);
        assertThat(client.providerName()).isEqualTo("ollama");
    }
}
