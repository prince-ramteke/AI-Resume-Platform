package com.princeramteke.resumeai.resume.dto;

public record ResumeMetadataResponse(
        String contentType,
        long fileSize,
        Integer pageCount,
        String language
) {
}
