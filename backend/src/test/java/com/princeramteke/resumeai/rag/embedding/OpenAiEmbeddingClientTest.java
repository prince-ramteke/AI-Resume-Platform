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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiEmbeddingClientTest {

    private static final String URL = "https://api.openai.com/v1/embeddings";

    private MockRestServiceServer server;
    private OpenAiEmbeddingClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        var config = new EmbeddingConfig("openai", 3, null,
                new EmbeddingConfig.OpenAi("https://api.openai.com", "text-embedding-3-small", "test-key"), null);
        client = new OpenAiEmbeddingClient(builder, config);
    }

    @Test
    void embedBatch_validResponse_returnsVectorsAndSendsAuthHeader() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess(
                        "{\"data\":[{\"embedding\":[0.1,0.2,0.3]},{\"embedding\":[0.4,0.5,0.6]}]}",
                        MediaType.APPLICATION_JSON));

        List<float[]> result = client.embedBatch(List.of("a", "b"));

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsExactly(0.1f, 0.2f, 0.3f);
        assertThat(result.get(1)).containsExactly(0.4f, 0.5f, 0.6f);
        server.verify();
    }

    @Test
    void embed_singleText_returnsFirstVector() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("{\"data\":[{\"embedding\":[0.7,0.8,0.9]}]}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.embed("hello")).containsExactly(0.7f, 0.8f, 0.9f);
    }

    @Test
    void embedBatch_dimensionMismatch_throwsEmbeddingException() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("{\"data\":[{\"embedding\":[0.1,0.2]}]}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.embed("hello"))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("dimension mismatch");
    }

    @Test
    void embedBatch_providerError_throwsEmbeddingException() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.embed("hello"))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("OpenAI");
    }

    @Test
    void providerName_isOpenai() {
        assertThat(client.providerName()).isEqualTo("openai");
    }
}
