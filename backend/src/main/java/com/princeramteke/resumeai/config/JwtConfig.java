package com.princeramteke.resumeai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtConfig(String secret, int expiryMinutes) {

    public JwtConfig {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 256 bits (32 characters)");
        }
        if (expiryMinutes <= 0) {
            expiryMinutes = 60;
        }
    }
}
