package com.princeramteke.resumeai.jobdescription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateJobDescriptionRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 50000) String rawText
) {}
