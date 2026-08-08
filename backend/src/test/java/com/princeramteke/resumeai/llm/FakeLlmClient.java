package com.princeramteke.resumeai.llm;

/**
 * Deterministic test double for {@link LlmClient}: returns canned content and fixed token
 * counts with no network. Mirrors the pattern in docs/TESTING.md so synthesis and (later)
 * service tests can drive the pipeline with known model output.
 */
public class FakeLlmClient implements LlmClient {

    private final String cannedContent;
    private final int promptTokens;
    private final int completionTokens;

    public FakeLlmClient(String cannedContent) {
        this(cannedContent, 0, 0);
    }

    public FakeLlmClient(String cannedContent, int promptTokens, int completionTokens) {
        this.cannedContent = cannedContent;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        return new LlmResponse(cannedContent, promptTokens, completionTokens);
    }

    @Override
    public String providerName() {
        return "fake";
    }
}
