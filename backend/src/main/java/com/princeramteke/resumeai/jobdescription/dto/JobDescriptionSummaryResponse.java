package com.princeramteke.resumeai.jobdescription.dto;

import java.time.Instant;

public record JobDescriptionSummaryResponse(
        Long id,
        String title,
        String contentType,
        Long fileSize,
        Instant createdAt
) {}
