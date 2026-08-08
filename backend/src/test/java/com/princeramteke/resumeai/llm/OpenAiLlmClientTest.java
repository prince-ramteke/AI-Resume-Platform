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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiLlmClientTest {

    private static final String URL = "https://api.openai.com/v1/chat/completions";

    private MockRestServiceServer server;
    private OpenAiLlmClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        var config = new LlmConfig("openai", 0.0, 42, null,
                new LlmConfig.OpenAi("https://api.openai.com", "gpt-4o-mini", "test-key"));
        client = new OpenAiLlmClient(builder, config);
    }

    @Test
    void complete_validResponse_returnsContentUsageAndSendsAuthHeader() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("gpt-4o-mini"))
                .andExpect(jsonPath("$.temperature").value(0.0))
                .andExpect(jsonPath("$.seed").value(42))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"{\\\"score\\\":72}\"}}],"
                                + "\"usage\":{\"prompt_tokens\":210,\"completion_tokens\":33}}",
                        MediaType.APPLICATION_JSON));

        LlmResponse response = client.complete(new LlmRequest("system instr", "user prompt"));

        assertThat(response.content()).isEqualTo("{\"score\":72}");
        assertThat(response.promptTokens()).isEqualTo(210);
        assertThat(response.completionTokens()).isEqualTo(33);
        server.verify();
    }

    @Test
    void complete_emptyChoices_throwsLlmException() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.complete(new LlmRequest("s", "u")))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void complete_providerError_throwsLlmException() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.complete(new LlmRequest("s", "u")))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("OpenAI");
    }

    @Test
    void providerName_isOpenai() {
        assertThat(client.providerName()).isEqualTo("openai");
    }
}
