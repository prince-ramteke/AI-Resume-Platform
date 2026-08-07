package com.princeramteke.resumeai.jobdescription.dto;

import java.time.Instant;

public record JobDescriptionResponse(
        Long id,
        String title,
        String rawText,
        String contentType,
        Long fileSize,
        Integer pageCount,
        String language,
        Instant createdAt,
        Instant updatedAt
) {}
