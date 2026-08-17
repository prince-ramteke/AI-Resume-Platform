package com.princeramteke.resumeai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.otp")
public record OtpConfig(
        @DefaultValue("10") int expiryMinutes,
        @DefaultValue("5") int maxAttempts,
        @DefaultValue("60") int resendCooldownSeconds
) {
}
