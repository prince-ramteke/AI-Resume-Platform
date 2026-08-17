package com.princeramteke.resumeai.notification;

import com.princeramteke.resumeai.config.FeatureFlags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class ResendEmailClient implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailClient.class);

    // Delays between successive attempts: [0, 500, 1000, 2000] ms.
    // Attempt 1 is immediate; attempts 2-4 wait the corresponding delay before retrying.
    private static final long[] DEFAULT_RETRY_DELAYS_MS = {0L, 500L, 1000L, 2000L};

    private final RestClient restClient;
    private final NotificationConfig config;
    private final FeatureFlags featureFlags;
    private final long[] retryDelaysMs;

    @Autowired
    public ResendEmailClient(RestClient.Builder builder,
                             NotificationConfig config,
                             FeatureFlags featureFlags) {
        this(builder, config, featureFlags, DEFAULT_RETRY_DELAYS_MS);
    }

    // Package-private: allows tests to inject zero delays for deterministic, fast execution.
    ResendEmailClient(RestClient.Builder builder,
                      NotificationConfig config,
                      FeatureFlags featureFlags,
                      long[] retryDelaysMs) {
        this.config = config;
        this.featureFlags = featureFlags;
        this.retryDelaysMs = retryDelaysMs;
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
            sendWithRetry(to, "You're in — welcome to Resume Intelligence",
                    EmailTemplates.welcomeEmail(firstName, config.frontendBaseUrl()), "Welcome");
        } catch (Exception ex) {
            log.warn("Welcome email delivery failed permanently to={}: {}", to, ex.getMessage());
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

    /**
     * Calls {@link #send} and retries on transient transport failures ({@link ResourceAccessException}).
     * HTTP-level errors (4xx/5xx) are not retried — they are permanent and retrying would be wasteful.
     * All attempts exhausted → rethrows the last exception for the caller to log and swallow.
     */
    private void sendWithRetry(String to, String subject, String html, String label) {
        ResourceAccessException lastTransientFailure = null;
        for (int attempt = 0; attempt < retryDelaysMs.length; attempt++) {
            if (attempt > 0) {
                long delayMs = retryDelaysMs[attempt];
                if (delayMs > 0) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during email retry", ie);
                    }
                }
                log.warn("{} email retry attempt={} to={}", label, attempt + 1, to);
            }
            try {
                send(to, subject, html);
                return; // success
            } catch (ResourceAccessException ex) {
                // Transient I/O failure — eligible for retry.
                lastTransientFailure = ex;
                log.warn("{} email transient failure attempt={} to={}: {}", label, attempt + 1, to, ex.getMessage());
            }
            // Any other exception (HttpClientErrorException, HttpServerErrorException, etc.)
            // propagates immediately — no retry for permanent HTTP errors.
        }
        throw lastTransientFailure;
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
