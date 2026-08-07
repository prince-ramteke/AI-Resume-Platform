package com.princeramteke.resumeai.rag.exception;

/**
 * Thrown when an embedding provider call fails or returns an unusable result.
 * Extends {@link RuntimeException} so it is handled centrally by the global
 * exception handler once an HTTP surface exists (M5).
 */
public class EmbeddingException extends RuntimeException {

    public EmbeddingException(String message) {
        super(message);
    }

    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }
}
