package com.princeramteke.resumeai.llm;

/**
 * A completion result: the raw model {@code content} (expected to be JSON for this
 * application's analysis use) plus token usage for observability. Parsing and validating the
 * content is a later concern — this carrier holds exactly what the provider returned.
 */
public record LlmResponse(String content, int promptTokens, int completionTokens) {
}
