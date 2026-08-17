package com.princeramteke.resumeai.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.notification")
public record NotificationConfig(
        String brevoApiKey,
        String brevoSenderEmail,
        @DefaultValue("Resume Intelligence") String brevoSenderName,
        String adminEmail,
        @DefaultValue("http://localhost:5173") String frontendBaseUrl,
        @DefaultValue("https://api.brevo.com") String brevoBaseUrl
) {
}
