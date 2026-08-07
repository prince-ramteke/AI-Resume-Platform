package com.princeramteke.resumeai.analysis.dto;

import java.time.Instant;

/**
 * Compact analysis row for the paginated history list ({@code GET /api/analyses}, see API.md §5).
 * {@code jobTitle} is denormalized from the associated job description for display.
 */
public record AnalysisSummaryResponse(
        Long id,
        int score,
        String jobTitle,
        Instant createdAt
) {
}
