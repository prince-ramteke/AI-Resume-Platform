package com.princeramteke.resumeai.analysis.exception;

/**
 * Thrown when an analysis does not exist or is not owned by the caller. Mapped centrally to
 * {@code 404} — non-owners get 404 (not 403) to prevent enumeration (see SECURITY.md §2).
 */
public class AnalysisNotFoundException extends RuntimeException {

    public AnalysisNotFoundException(Long id) {
        super("Analysis not found: " + id);
    }
}
