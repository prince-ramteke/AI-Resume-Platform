package com.princeramteke.resumeai.auth.dto;

import java.time.Instant;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        String refreshToken,
        Instant refreshExpiresAt
) {

    public LoginResponse(String accessToken, Instant expiresAt, String refreshToken, Instant refreshExpiresAt) {
        this(accessToken, "Bearer", expiresAt, refreshToken, refreshExpiresAt);
    }
}
