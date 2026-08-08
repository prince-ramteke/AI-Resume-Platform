package com.princeramteke.resumeai.llm;

import com.princeramteke.resumeai.llm.exception.LlmException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OllamaLlmClientTest {

    private static final String URL = "http://localhost:11434/api/chat";

    private MockRestServiceServer server;
    private OllamaLlmClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        var config = new LlmConfig("ollama", 0.0, 42,
                new LlmConfig.Ollama("http://localhost:11434", "llama3.1:8b"), null);
        client = new OllamaLlmClient(builder, config);
    }

    @Test
    void complete_validResponse_returnsContentAndUsage() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.model").value("llama3.1:8b"))
                .andExpect(jsonPath("$.stream").value(false))
                .andExpect(jsonPath("$.format").value("json"))
                .andExpect(jsonPath("$.options.temperature").value(0.0))
                .andExpect(jsonPath("$.options.seed").value(42))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[0].content").value("system instr"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[1].content").value("user prompt"))
                .andRespond(withSuccess(
                        "{\"message\":{\"role\":\"assistant\",\"content\":\"{\\\"score\\\":80}\"},"
                                + "\"prompt_eval_count\":120,\"eval_count\":45}",
                        MediaType.APPLICATION_JSON));

        LlmResponse response = client.complete(new LlmRequest("system instr", "user prompt"));

        assertThat(response.content()).isEqualTo("{\"score\":80}");
        assertThat(response.promptTokens()).isEqualTo(120);
        assertThat(response.completionTokens()).isEqualTo(45);
        server.verify();
    }

    @Test
    void complete_missingUsage_defaultsTokensToZero() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess(
                        "{\"message\":{\"role\":\"assistant\",\"content\":\"{}\"}}",
                        MediaType.APPLICATION_JSON));

        LlmResponse response = client.complete(new LlmRequest("s", "u"));

        assertThat(response.content()).isEqualTo("{}");
        assertThat(response.promptTokens()).isZero();
        assertThat(response.completionTokens()).isZero();
    }

    @Test
    void complete_emptyContent_throwsLlmException() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess(
                        "{\"message\":{\"role\":\"assistant\",\"content\":\"\"}}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.complete(new LlmRequest("s", "u")))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void complete_providerError_throwsLlmException() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.complete(new LlmRequest("s", "u")))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("Ollama");
    }

    @Test
    void providerName_isOllama() {
        assertThat(client.providerName()).isEqualTo("ollama");
    }
}
