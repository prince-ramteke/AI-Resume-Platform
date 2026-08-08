package com.princeramteke.resumeai.analysis.exception;

/**
 * Thrown when the analysis pipeline cannot produce a usable verdict — the model's output is
 * unparseable or structurally invalid even after the single repair-retry. Mapped centrally to
 * {@code 422} (see API.md §1): the request was well-formed but the LLM result is unusable.
 */
public class AnalysisFailedException extends RuntimeException {

    public AnalysisFailedException(String message) {
        super(message);
    }

    public AnalysisFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
