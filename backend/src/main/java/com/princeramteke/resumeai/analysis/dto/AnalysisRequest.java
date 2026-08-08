package com.princeramteke.resumeai.analysis.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request to analyze a resume against a job description. Both IDs must reference resources owned
 * by the caller (enforced in the service). Validation failures return {@code 400}.
 */
public record AnalysisRequest(
        @NotNull @Positive Long resumeId,
        @NotNull @Positive Long jobDescriptionId
) {
}
