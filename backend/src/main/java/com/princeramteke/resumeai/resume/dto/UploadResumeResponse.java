package com.princeramteke.resumeai.resume.dto;

import java.time.Instant;

public record UploadResumeResponse(
        Long id,
        String filename,
        String contentType,
        long fileSize,
        Instant createdAt
) {
}
