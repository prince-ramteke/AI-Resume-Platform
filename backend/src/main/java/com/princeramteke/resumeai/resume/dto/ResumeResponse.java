package com.princeramteke.resumeai.resume.dto;

import java.time.Instant;

public record ResumeResponse(
        Long id,
        String filename,
        String contentType,
        long fileSize,
        String rawText,
        Integer pageCount,
        String language,
        Instant createdAt,
        Instant updatedAt
) {
}
