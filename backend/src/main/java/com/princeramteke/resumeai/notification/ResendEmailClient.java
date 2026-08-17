package com.princeramteke.resumeai.notification;

import com.princeramteke.resumeai.config.FeatureFlags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class ResendEmailClient implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailClient.class);

    private final RestClient restClient;
    private final NotificationConfig config;
    private final FeatureFlags featureFlags;

    public ResendEmailClient(RestClient.Builder builder,
                             NotificationConfig config,
                             FeatureFlags featureFlags) {
        this.config = config;
        this.featureFlags = featureFlags;
        this.restClient = builder.baseUrl(config.resendBaseUrl()).build();
    }

    @Override
    public void sendOtpEmail(String to, String firstName, String otp, int expiryMinutes) {
        if (!featureFlags.notificationEnabled()) return;
        try {
            send(to, "Your verification code", EmailTemplates.otpEmail(firstName, otp, expiryMinutes));
        } catch (Exception ex) {
            log.warn("OTP email delivery failed to={}: {}", to, ex.getMessage());
        }
    }

    @Override
    public void sendWelcomeEmail(String to, String firstName) {
        if (!featureFlags.notificationEnabled()) return;
        try {
            send(to, "You're in — welcome to Resume Intelligence",
                    EmailTemplates.welcomeEmail(firstName, config.frontendBaseUrl()));
        } catch (Exception ex) {
            log.warn("Welcome email delivery failed to={}: {}", to, ex.getMessage());
        }
    }

    @Override
    public void sendAdminNotification(String firstName, String lastName, String email,
                                      String provider, Instant registeredAt, Long userId) {
        if (!featureFlags.notificationEnabled()) return;
        if (config.adminEmail() == null || config.adminEmail().isBlank()) {
            log.debug("Admin notification skipped: admin email not configured");
            return;
        }
        try {
            send(config.adminEmail(), "New user registered: " + email,
                    EmailTemplates.adminNotification(firstName, lastName, email, provider, registeredAt, userId));
        } catch (Exception ex) {
            log.warn("Admin notification delivery failed for userId={}: {}", userId, ex.getMessage());
        }
    }

    private void send(String to, String subject, String html) {
        Map<String, Object> body = Map.of(
                "from", config.senderAddress(),
                "to", List.of(to),
                "subject", subject,
                "html", html
        );
        restClient.post()
                .uri("/emails")
                .header("Authorization", "Bearer " + config.resendApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
