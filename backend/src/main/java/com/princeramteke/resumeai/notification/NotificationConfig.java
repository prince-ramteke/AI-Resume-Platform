package com.princeramteke.resumeai.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration for transactional email delivery via Resend.
 *
 * All fields default to empty strings so the application starts without error
 * when Resend is not yet configured. The FeatureFlags.notificationEnabled guard
 * prevents any outbound calls when the flag is false, so an empty API key is
 * never used in practice during local development.
 */
@ConfigurationProperties(prefix = "app.notification")
public record NotificationConfig(
        @DefaultValue("") String resendApiKey,
        @DefaultValue("") String senderAddress,
        @DefaultValue("") String adminEmail
) {
}
