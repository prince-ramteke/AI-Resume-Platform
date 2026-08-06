package com.princeramteke.resumeai.common.dto;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String traceId
) {
    public static ErrorResponse of(int status, String error, String message, String traceId) {
        return new ErrorResponse(Instant.now(), status, error, message, traceId);
    }
}
