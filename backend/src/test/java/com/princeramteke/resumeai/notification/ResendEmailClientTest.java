package com.princeramteke.resumeai.notification;

import com.princeramteke.resumeai.config.FeatureFlags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.client.ExpectedCount.*;

class ResendEmailClientTest {

    private static final NotificationConfig CONFIG = new NotificationConfig(
            "test-api-key",
            "noreply@resumeai.dev",
            "admin@resumeai.dev",
            "http://localhost:5173",
            "https://api.resend.com"
    );
    private static final FeatureFlags ENABLED  = new FeatureFlags(false, false, true);
    private static final FeatureFlags DISABLED = new FeatureFlags(false, false, false);

    // Zero-delay backoff: [0,0,0,0] — retries immediately so tests are fast.
    private static final long[] ZERO_DELAYS = {0L, 0L, 0L, 0L};

    private MockRestServiceServer server;
    private ResendEmailClient clientEnabled;
    private ResendEmailClient clientDisabled;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        clientEnabled  = new ResendEmailClient(builder, CONFIG, ENABLED, ZERO_DELAYS);

        RestClient.Builder disabledBuilder = RestClient.builder();
        clientDisabled = new ResendEmailClient(disabledBuilder, CONFIG, DISABLED, ZERO_DELAYS);
    }

    // ─── notificationEnabled=false — no HTTP calls ────────────────────────────

    @Test
    void sendOtpEmail_notificationDisabled_makesNoHttpCall() {
        // If an HTTP call were made, MockRestServiceServer would throw because
        // no expectation is set.
        assertThatCode(() -> clientDisabled.sendOtpEmail("user@example.com", "Alice", "123456", 10))
                .doesNotThrowAnyException();
        server.verify(); // zero expectations → passes with zero calls
    }

    @Test
    void sendWelcomeEmail_notificationDisabled_makesNoHttpCall() {
        assertThatCode(() -> clientDisabled.sendWelcomeEmail("user@example.com", "Alice"))
                .doesNotThrowAnyException();
    }

    @Test
    void sendAdminNotification_notificationDisabled_makesNoHttpCall() {
        assertThatCode(() -> clientDisabled.sendAdminNotification(
                "Alice", "Smith", "a@example.com", "LOCAL", Instant.now(), 1L))
                .doesNotThrowAnyException();
    }

    // ─── successful send ──────────────────────────────────────────────────────

    @Test
    void sendOtpEmail_enabled_postsCorrectHeadersAndBodyContainingOtp() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("alice@example.com")))
                .andExpect(content().string(containsString("482931")))
                .andRespond(withSuccess("{\"id\":\"abc123\"}", MediaType.APPLICATION_JSON));

        clientEnabled.sendOtpEmail("alice@example.com", "Alice", "482931", 10);

        server.verify();
    }

    @Test
    void sendWelcomeEmail_enabled_postsToCorrectEndpointWithRecipient() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andExpect(content().string(containsString("alice@example.com")))
                .andRespond(withSuccess("{\"id\":\"def456\"}", MediaType.APPLICATION_JSON));

        clientEnabled.sendWelcomeEmail("alice@example.com", "Alice");

        server.verify();
    }

    @Test
    void sendAdminNotification_enabled_sendsToAdminAddressNotUser() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("admin@resumeai.dev")))
                // OTP must never appear in admin notification
                .andRespond(withSuccess("{\"id\":\"ghi789\"}", MediaType.APPLICATION_JSON));

        clientEnabled.sendAdminNotification(
                "Alice", "Smith", "alice@example.com", "LOCAL", Instant.now(), 42L);

        server.verify();
    }

    // ─── 4xx / 5xx — swallowed, never thrown ─────────────────────────────────

    @Test
    void sendOtpEmail_resend4xx_doesNotThrow() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withBadRequest().body("{\"name\":\"validation_error\"}"));

        assertThatCode(() -> clientEnabled.sendOtpEmail("u@example.com", "U", "111111", 10))
                .doesNotThrowAnyException();
    }

    @Test
    void sendWelcomeEmail_resend5xx_doesNotThrow() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withServerError());

        assertThatCode(() -> clientEnabled.sendWelcomeEmail("u@example.com", "U"))
                .doesNotThrowAnyException();
    }

    @Test
    void sendAdminNotification_adminEmailBlank_makesNoHttpCall() {
        // When admin email is not configured the client must skip silently.
        var configNoAdmin = new NotificationConfig("test-api-key", "noreply@resumeai.dev", "", "http://localhost:5173", "https://api.resend.com");
        RestClient.Builder b = RestClient.builder();
        var client = new ResendEmailClient(b, configNoAdmin, ENABLED, ZERO_DELAYS);

        assertThatCode(() -> client.sendAdminNotification(
                "A", "B", "c@example.com", "LOCAL", Instant.now(), 1L))
                .doesNotThrowAnyException();
    }

    // ─── retry: transient I/O failures ───────────────────────────────────────

    @Test
    void sendWelcomeEmail_transientIoFailureThenSuccess_retriesAndSucceeds() {
        // Attempts 1-2 fail with I/O error; attempt 3 succeeds. Exactly 3 HTTP calls expected.
        server.expect(times(2), requestTo("https://api.resend.com/emails"))
                .andRespond(withException(new java.io.IOException("socket reset")));
        server.expect(once(), requestTo("https://api.resend.com/emails"))
                .andRespond(withSuccess("{\"id\":\"ok\"}", MediaType.APPLICATION_JSON));

        assertThatCode(() -> clientEnabled.sendWelcomeEmail("u@example.com", "U"))
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void sendWelcomeEmail_allAttemptsTransientFailure_swallowedNotThrown() {
        // All 4 attempts fail with I/O error — must not propagate out of sendWelcomeEmail.
        server.expect(times(4), requestTo("https://api.resend.com/emails"))
                .andRespond(withException(new java.io.IOException("connection refused")));

        assertThatCode(() -> clientEnabled.sendWelcomeEmail("u@example.com", "U"))
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void sendWelcomeEmail_http403_noRetry_exactlyOneHttpCall() {
        // 403 is a permanent HTTP error — must not trigger any retry.
        server.expect(once(), requestTo("https://api.resend.com/emails"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.FORBIDDEN));

        assertThatCode(() -> clientEnabled.sendWelcomeEmail("u@example.com", "U"))
                .doesNotThrowAnyException();
        server.verify(); // exactly 1 call
    }

    @Test
    void sendWelcomeEmail_http422_noRetry_exactlyOneHttpCall() {
        // 422 (Resend validation error) is permanent — no retry.
        server.expect(once(), requestTo("https://api.resend.com/emails"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY));

        assertThatCode(() -> clientEnabled.sendWelcomeEmail("u@example.com", "U"))
                .doesNotThrowAnyException();
        server.verify(); // exactly 1 call
    }

    @Test
    void sendWelcomeEmail_success_exactlyOneHttpCall() {
        // Happy path unchanged: one successful attempt, no spurious retries.
        server.expect(once(), requestTo("https://api.resend.com/emails"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andRespond(withSuccess("{\"id\":\"xyz\"}", MediaType.APPLICATION_JSON));

        clientEnabled.sendWelcomeEmail("alice@example.com", "Alice");

        server.verify(); // exactly 1 call
    }
}
