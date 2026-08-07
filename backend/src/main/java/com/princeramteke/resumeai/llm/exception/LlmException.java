package com.princeramteke.resumeai.llm.exception;

/**
 * Thrown when an LLM provider call fails or returns an unusable result. Extends
 * {@link RuntimeException} so it is handled centrally by the global exception handler once the
 * analysis HTTP surface exists (M5.3). Mirrors
 * {@link com.princeramteke.resumeai.rag.exception.EmbeddingException}.
 */
public class LlmException extends RuntimeException {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
