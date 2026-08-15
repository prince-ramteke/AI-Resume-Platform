package com.princeramteke.resumeai.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
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
 * Fallback (cloud) chat provider. Calls OpenAI's {@code /v1/chat/completions} endpoint via a
 * thin {@link RestClient}. Active only when {@code app.llm.provider} is {@code openai}. Requests
 * a JSON-object response format to match the structured-output contract. Sending document text
 * to OpenAI is an opt-in, disclosed choice (see SECURITY.md). Generation parameters come from
 * {@link LlmConfig}; the request supplies only the prompt content.
 */
@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "openai")
public class OpenAiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiLlmClient.class);

    private final RestClient restClient;
    private final LlmConfig config;

    public OpenAiLlmClient(RestClient.Builder builder, LlmConfig config) {
        this.config = config;
        this.restClient = builder.baseUrl(config.openai().baseUrl()).build();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        log.info("Chat completion started: provider=openai, model={}", config.openai().model());
        try {
            boolean isGemini = config.openai().baseUrl().contains("googleapis.com");
            OpenAiChatRequest body = new OpenAiChatRequest(
                    config.openai().model(),
                    List.of(new OpenAiChatRequest.Message("system", request.systemPrompt()),
                            new OpenAiChatRequest.Message("user", request.userPrompt())),
                    config.temperature(),
                    isGemini ? null : config.seed(),
                    new OpenAiChatRequest.ResponseFormat("json_object"));

            OpenAiChatResponse response = restClient.post()
                    .uri("/v1/chat/completions")
                    .header("Authorization", "Bearer " + config.openai().apiKey())
                    .body(body)
                    .retrieve()
                    .body(OpenAiChatResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()
                    || response.choices().get(0).message() == null
                    || response.choices().get(0).message().content() == null
                    || response.choices().get(0).message().content().isBlank()) {
                throw new LlmException("OpenAI returned an empty completion");
            }

            String content = response.choices().get(0).message().content();
            int promptTokens = response.usage() != null ? response.usage().promptTokens() : 0;
            int completionTokens = response.usage() != null ? response.usage().completionTokens() : 0;
            log.info("Chat completion finished: provider=openai, promptTokens={}, completionTokens={}",
                    promptTokens, completionTokens);
            return new LlmResponse(content, promptTokens, completionTokens);
        } catch (RestClientException e) {
            throw new LlmException("Failed to call OpenAI chat API", e);
        }
    }

    @Override
    public String providerName() {
        return "openai";
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record OpenAiChatRequest(String model, List<Message> messages, double temperature,
                             Integer seed,
                             @JsonProperty("response_format") ResponseFormat responseFormat) {
        record Message(String role, String content) {
        }

        record ResponseFormat(String type) {
        }
    }

    record OpenAiChatResponse(List<Choice> choices, Usage usage) {
        record Choice(Message message) {
        }

        record Message(String role, String content) {
        }

        record Usage(@JsonProperty("prompt_tokens") int promptTokens,
                     @JsonProperty("completion_tokens") int completionTokens) {
        }
    }
}
