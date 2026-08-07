package com.princeramteke.resumeai.llm;

/**
 * Content for a single completion: the isolated system instruction and the user prompt.
 * Generation parameters (temperature, seed) are configuration rather than per-call content and
 * are applied by the client from {@link LlmConfig}. Keeping document/user text out of the
 * system field preserves the instruction/data separation required for prompt-injection safety.
 */
public record LlmRequest(String systemPrompt, String userPrompt) {
}
