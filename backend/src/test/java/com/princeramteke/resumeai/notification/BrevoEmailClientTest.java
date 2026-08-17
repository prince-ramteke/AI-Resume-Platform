package com.princeramteke.resumeai.notification;

import com.princeramteke.resumeai.config.FeatureFlags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.ExpectedCount.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class BrevoEmailClientTest {

    // Uses a placeholder key — never a production secret.
    private static final NotificationConfig CONFIG = new NotificationConfig(
            "test-brevo-key",
            "noreply@resumeai.dev",
            "Resume Intelligence",
            "admin@resumeai.dev",
            "http://localhost:5173",
            "https://api.brevo.com"
    );
    private static final FeatureFlags ENABLED  = new FeatureFlags(false, false, true);
    private static final FeatureFlags DISABLED = new FeatureFlags(false, false, false);

    // Zero-delay backoff: retries immediately so tests are fast.
    private static final long[] ZERO_DELAYS = {0L, 0L, 0L, 0L};

    private MockRestServiceServer server;
    private BrevoEmailClient clientEnabled;
    private BrevoEmailClient clientDisabled;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        clientEnabled = new BrevoEmailClient(builder, CONFIG, ENABLED, ZERO_DELAYS);

        RestClient.Builder disabledBuilder = RestClient.builder();
        clientDisabled = new BrevoEmailClient(disabledBuilder, CONFIG, DISABLED, ZERO_DELAYS);
    }

    // ─── notificationEnabled=false — no HTTP calls ────────────────────────────

    @Test
    void sendOtpEmail_notificationDisabled_makesNoHttpCall() {
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

    // ─── correct Brevo endpoint and API-key header ────────────────────────────

    @Test
    void sendOtpEmail_enabled_postsToBrevoSmtpEndpoint() {
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "test-brevo-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("alice@example.com")))
                .andExpect(content().string(containsString("482931")))
                .andRespond(withSuccess("{\"messageId\":\"<msg1@brevo>\"}", MediaType.APPLICATION_JSON));

        clientEnabled.sendOtpEmail("alice@example.com", "Alice", "482931", 10);

        server.verify();
    }

    @Test
    void sendOtpEmail_enabled_payloadUsesBrevoSenderStructureAndHtmlContent() {
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(content().string(containsString("\"sender\"")))
                .andExpect(content().string(containsString("noreply@resumeai.dev")))
                .andExpect(content().string(containsString("Resume Intelligence")))
                .andExpect(content().string(containsString("\"htmlContent\"")))
                .andRespond(withSuccess("{\"messageId\":\"<msg2@brevo>\"}", MediaType.APPLICATION_JSON));

        clientEnabled.sendOtpEmail("alice@example.com", "Alice", "111111", 10);

        server.verify();
    }

    @Test
    void sendOtpEmail_senderNameAndEmailForwardedCorrectly() {
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(content().string(containsString("Resume Intelligence")))
                .andExpect(content().string(containsString("noreply@resumeai.dev")))
                .andRespond(withSuccess("{\"messageId\":\"<msg3@brevo>\"}", MediaType.APPLICATION_JSON));

        clientEnabled.sendOtpEmail("alice@example.com", "Alice", "654321", 10);

        server.verify();
    }

    @Test
    void sendWelcomeEmail_enabled_postsToBrevoEndpointWithRecipient() {
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "test-brevo-key"))
                .andExpect(content().string(containsString("alice@example.com")))
                .andRespond(withSuccess("{\"messageId\":\"<msg4@brevo>\"}", MediaType.APPLICATION_JSON));

        clientEnabled.sendWelcomeEmail("alice@example.com", "Alice");

        server.verify();
    }

    @Test
    void sendAdminNotification_enabled_sendsToAdminAddressNotUser() {
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("admin@resumeai.dev")))
                .andRespond(withSuccess("{\"messageId\":\"<msg5@brevo>\"}", MediaType.APPLICATION_JSON));

        clientEnabled.sendAdminNotification(
                "Alice", "Smith", "alice@example.com", "LOCAL", Instant.now(), 42L);

        server.verify();
    }

    @Test
    void sendOtpEmail_apiKeyNeverAppearsInRequestBody() {
        // The API key must be in the header only, never in the JSON payload.
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(content().string(not(containsString("test-brevo-key"))))
                .andRespond(withSuccess("{\"messageId\":\"<msg6@brevo>\"}", MediaType.APPLICATION_JSON));

        clientEnabled.sendOtpEmail("alice@example.com", "Alice", "999999", 10);

        server.verify();
    }

    // ─── 4xx / 5xx — swallowed, never thrown ─────────────────────────────────

    @Test
    void sendOtpEmail_brevo4xx_doesNotThrow() {
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andRespond(withBadRequest().body("{\"code\":\"invalid_parameter\",\"message\":\"bad\"}"));

        assertThatCode(() -> clientEnabled.sendOtpEmail("u@example.com", "U", "111111", 10))
                .doesNotThrowAnyException();
    }

    @Test
    void sendWelcomeEmail_brevo5xx_doesNotThrow() {
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andRespond(withServerError());

        assertThatCode(() -> clientEnabled.sendWelcomeEmail("u@example.com", "U"))
                .doesNotThrowAnyException();
    }

    @Test
    void sendAdminNotification_adminEmailBlank_makesNoHttpCall() {
        var configNoAdmin = new NotificationConfig(
                "test-brevo-key", "noreply@resumeai.dev", "Resume Intelligence",
                "", "http://localhost:5173", "https://api.brevo.com");
        RestClient.Builder b = RestClient.builder();
        var client = new BrevoEmailClient(b, configNoAdmin, ENABLED, ZERO_DELAYS);

        assertThatCode(() -> client.sendAdminNotification(
                "A", "B", "c@example.com", "LOCAL", Instant.now(), 1L))
                .doesNotThrowAnyException();
    }

    // ─── retry: transient I/O failures ───────────────────────────────────────

    @Test
    void sendWelcomeEmail_transientIoFailureThenSuccess_retriesAndSucceeds() {
        // Attempts 1-2 fail with I/O error; attempt 3 succeeds. Exactly 3 HTTP calls expected.
        server.expect(times(2), requestTo("https://api.brevo.com/v3/smtp/email"))
                .andRespond(withException(new java.io.IOException("socket reset")));
        server.expect(once(), requestTo("https://api.brevo.com/v3/smtp/email"))
                .andRespond(withSuccess("{\"messageId\":\"<ok@brevo>\"}", MediaType.APPLICATION_JSON));

        assertThatCode(() -> clientEnabled.sendWelcomeEmail("u@example.com", "U"))
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void sendWelcomeEmail_allAttemptsTransientFailure_swallowedNotThrown() {
        // All 4 attempts fail with I/O error — must not propagate out of sendWelcomeEmail.
        server.expect(times(4), requestTo("https://api.brevo.com/v3/smtp/email"))
                .andRespond(withException(new java.io.IOException("connection refused")));

        assertThatCode(() -> clientEnabled.sendWelcomeEmail("u@example.com", "U"))
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void sendWelcomeEmail_http401_noRetry_exactlyOneHttpCall() {
        // 401 Unauthorized is a permanent auth failure — must not trigger any retry.
        server.expect(once(), requestTo("https://api.brevo.com/v3/smtp/email"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatCode(() -> clientEnabled.sendWelcomeEmail("u@example.com", "U"))
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void sendWelcomeEmail_http403_noRetry_exactlyOneHttpCall() {
        server.expect(once(), requestTo("https://api.brevo.com/v3/smtp/email"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatCode(() -> clientEnabled.sendWelcomeEmail("u@example.com", "U"))
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void sendWelcomeEmail_http400_noRetry_exactlyOneHttpCall() {
        server.expect(once(), requestTo("https://api.brevo.com/v3/smtp/email"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatCode(() -> clientEnabled.sendWelcomeEmail("u@example.com", "U"))
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void sendWelcomeEmail_success_exactlyOneHttpCall() {
        // Happy path: one successful attempt, no spurious retries.
        server.expect(once(), requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "test-brevo-key"))
                .andRespond(withSuccess("{\"messageId\":\"<xyz@brevo>\"}", MediaType.APPLICATION_JSON));

        clientEnabled.sendWelcomeEmail("alice@example.com", "Alice");

        server.verify();
    }
}
