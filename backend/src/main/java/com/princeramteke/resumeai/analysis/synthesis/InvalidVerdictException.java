package com.princeramteke.resumeai.analysis.synthesis;

/**
 * Thrown when the model's output cannot be turned into a usable {@link LlmVerdict}: it is not
 * valid JSON (even after one repair attempt), or it is structurally impossible (e.g. a passing
 * score with no grounded matched skills — a prompt-injection tell, see SECURITY.md §5.1).
 *
 * <p>Synthesis-internal on purpose: the analysis service (M5.3) catches this to drive the single
 * LLM repair-retry and, if the retry also fails, surfaces a {@code 422} at the HTTP boundary.
 */
public class InvalidVerdictException extends RuntimeException {

    public InvalidVerdictException(String message) {
        super(message);
    }

    public InvalidVerdictException(String message, Throwable cause) {
        super(message, cause);
    }
}
