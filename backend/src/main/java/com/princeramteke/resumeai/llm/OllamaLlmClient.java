package com.princeramteke.resumeai.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.princeramteke.resumeai.llm.exception.LlmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Default (local) chat provider. Calls Ollama's {@code /api/chat} endpoint via a thin
 * {@link RestClient}. Active when {@code app.llm.provider} is {@code ollama} or unset.
 * Requests {@code format: "json"} with streaming disabled so the response is a single JSON
 * object — hardening the structured-output contract the analysis engine depends on. Generation
 * parameters come from {@link LlmConfig}; the request supplies only the prompt content.
 */
@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaLlmClient.class);

    private final RestClient restClient;
    private final LlmConfig config;

    public OllamaLlmClient(RestClient.Builder builder, LlmConfig config) {
        this.config = config;
        this.restClient = builder.baseUrl(config.ollama().baseUrl()).build();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        log.info("Chat completion started: provider=ollama, model={}", config.ollama().model());
        try {
            OllamaChatRequest body = new OllamaChatRequest(
                    config.ollama().model(),
                    List.of(new OllamaChatRequest.Message("system", request.systemPrompt()),
                            new OllamaChatRequest.Message("user", request.userPrompt())),
                    false,
                    "json",
                    new OllamaChatRequest.Options(config.temperature(), config.seed()));

            OllamaChatResponse response = restClient.post()
                    .uri("/api/chat")
                    .body(body)
                    .retrieve()
                    .body(OllamaChatResponse.class);

            if (response == null || response.message() == null
                    || response.message().content() == null || response.message().content().isBlank()) {
                throw new LlmException("Ollama returned an empty completion");
            }

            int promptTokens = response.promptEvalCount() != null ? response.promptEvalCount() : 0;
            int completionTokens = response.evalCount() != null ? response.evalCount() : 0;
            log.info("Chat completion finished: provider=ollama, promptTokens={}, completionTokens={}",
                    promptTokens, completionTokens);
            return new LlmResponse(response.message().content(), promptTokens, completionTokens);
        } catch (RestClientException e) {
            throw new LlmException("Failed to call Ollama chat API", e);
        }
    }

    @Override
    public String providerName() {
        return "ollama";
    }

    record OllamaChatRequest(String model, List<Message> messages, boolean stream,
                             String format, Options options) {
        record Message(String role, String content) {
        }

        record Options(double temperature, Integer seed) {
        }
    }

    record OllamaChatResponse(Message message,
                              @JsonProperty("prompt_eval_count") Integer promptEvalCount,
                              @JsonProperty("eval_count") Integer evalCount) {
        record Message(String role, String content) {
        }
    }
}
