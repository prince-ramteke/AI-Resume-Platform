package com.princeramteke.resumeai.auth;

import com.princeramteke.resumeai.auth.dto.RegisterRequest;
import com.princeramteke.resumeai.auth.dto.ResendOtpRequest;
import com.princeramteke.resumeai.llm.FakeLlmClient;
import com.princeramteke.resumeai.llm.LlmClient;
import com.princeramteke.resumeai.rag.embedding.EmbeddingClient;
import com.princeramteke.resumeai.rag.embedding.FakeEmbeddingClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration tests for the registration/email-delivery failure boundary.
 *
 * <p>Proves that when Resend returns a non-2xx response (or any exception) during
 * OTP email send, the LOCAL user registration transaction still commits — the user
 * row and email_verifications row are durable in PostgreSQL even though email delivery
 * failed.
 *
 * <p>Resend is replaced by an in-process {@link HttpServer} started before the Spring
 * context boots. {@link DynamicPropertySource} points {@code app.notification.resend-base-url}
 * at the local server's port so no real API key or outbound call is ever made.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "app.embedding.provider=none",
                "app.llm.provider=none",
                "app.jwt.secret=test-secret-that-is-at-least-32-characters-long-for-hmac",
                "app.jwt.expiry-minutes=60",
                "app.feature.email-verification-enabled=true",
                "app.feature.notification-enabled=true",
                "app.otp.expiry-minutes=10",
                "app.otp.max-attempts=5",
                "app.otp.resend-cooldown-seconds=60"
        })
@Testcontainers
@Import(EmailDeliveryFailureIT.TestConfig.class)
class EmailDeliveryFailureIT {

    // ─── Mock Resend HTTP server ───────────────────────────────────────────────
    //
    // Must be started in the static initializer so the port is known before
    // @DynamicPropertySource is called (DynamicPropertySource fires before @BeforeAll).

    private static final AtomicInteger MOCK_STATUS = new AtomicInteger(200);
    private static final List<String> CAPTURED_BODIES = new CopyOnWriteArrayList<>();
    private static final HttpServer MOCK_RESEND;

    static {
        try {
            MOCK_RESEND = HttpServer.create(new InetSocketAddress(0), 0);
            MOCK_RESEND.createContext("/emails", exchange -> {
                byte[] req = exchange.getRequestBody().readAllBytes();
                CAPTURED_BODIES.add(new String(req, StandardCharsets.UTF_8));
                int status = MOCK_STATUS.get();
                byte[] resp = (status < 300)
                        ? "{\"id\":\"test-id\"}".getBytes(StandardCharsets.UTF_8)
                        : "{\"name\":\"test_error\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, resp.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(resp);
                }
            });
            MOCK_RESEND.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start mock Resend server", e);
        }
    }

    @AfterAll
    static void stopMockServer() {
        MOCK_RESEND.stop(0);
    }

    // ─── PostgreSQL container ─────────────────────────────────────────────────

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("resumeai")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        // Redirect all Resend HTTP calls to the local mock server — no real API key needed.
        registry.add("app.notification.resend-base-url",
                () -> "http://localhost:" + MOCK_RESEND.getAddress().getPort());
        registry.add("app.notification.resend-api-key", () -> "test-key");
        registry.add("app.notification.sender-address", () -> "noreply@test.dev");
        registry.add("app.notification.admin-email", () -> "admin@test.dev");
    }

    // ─── Spring beans ─────────────────────────────────────────────────────────

    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;
    @Autowired EmailVerificationRepository emailVerificationRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetState() {
        CAPTURED_BODIES.clear();
        MOCK_STATUS.set(200);
        emailVerificationRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    // ─── Failure boundary tests ───────────────────────────────────────────────

    @Test
    void register_resendNon2xx_transactionCommitsDespiteEmailFailure() {
        MOCK_STATUS.set(422);

        // register() must succeed and return normally even though Resend rejects.
        assertThatCode(() ->
                authService.register(new RegisterRequest("alice@example.com", "StrongPass1!", "Alice", null)))
                .doesNotThrowAnyException();

        // User row committed
        var saved = userRepository.findByEmail("alice@example.com");
        assertThat(saved).as("user must be persisted after registration").isPresent();
        assertThat(saved.get().isEmailVerified())
                .as("email must remain unverified until OTP is consumed").isFalse();

        // email_verifications row committed
        long evCount = emailVerificationRepository.findAll().stream()
                .filter(ev -> ev.getUser().getId().equals(saved.get().getId()))
                .count();
        assertThat(evCount)
                .as("email_verifications row must be committed despite email delivery failure")
                .isEqualTo(1);
    }

    @Test
    void register_resendNon2xx_userEmailVerifiedRemainsFlase() {
        MOCK_STATUS.set(500);

        authService.register(new RegisterRequest("bob@example.com", "StrongPass1!", null, null));

        assertThat(userRepository.findByEmail("bob@example.com").orElseThrow().isEmailVerified())
                .as("email_verified must be false after a failed OTP email")
                .isFalse();
    }

    @Test
    void register_resendNon2xx_subsequentResendCreatesNewRecord() {
        MOCK_STATUS.set(422); // first OTP email fails
        authService.register(new RegisterRequest("carol@example.com", "StrongPass1!", null, null));

        User user = userRepository.findByEmail("carol@example.com").orElseThrow();
        EmailVerification first = emailVerificationRepository.findMostRecentByUserId(user.getId()).orElseThrow();
        // Move the first record's created_at back 120 s so the 60-second cooldown gate passes.
        jdbcTemplate.update(
                "UPDATE email_verifications SET created_at = NOW() - INTERVAL '120 seconds' WHERE id = ?",
                first.getId());

        MOCK_STATUS.set(200); // resend succeeds
        var resendResp = authService.resendOtp(new ResendOtpRequest("carol@example.com"));

        assertThat(resendResp.message()).contains("registered");

        long count = emailVerificationRepository.findAll().stream()
                .filter(ev -> ev.getUser().getId().equals(user.getId()))
                .count();
        assertThat(count)
                .as("two OTP records must exist: original + resend")
                .isEqualTo(2);
    }

    @Test
    void resendOtp_resendNon2xx_resendRecordStillCommitted() {
        // Initial registration succeeds.
        MOCK_STATUS.set(200);
        authService.register(new RegisterRequest("dave@example.com", "StrongPass1!", null, null));

        User user = userRepository.findByEmail("dave@example.com").orElseThrow();
        EmailVerification first = emailVerificationRepository.findMostRecentByUserId(user.getId()).orElseThrow();
        jdbcTemplate.update(
                "UPDATE email_verifications SET created_at = NOW() - INTERVAL '120 seconds' WHERE id = ?",
                first.getId());

        // resendOtp with delivery failure must still commit the new OTP record.
        MOCK_STATUS.set(503);
        assertThatCode(() -> authService.resendOtp(new ResendOtpRequest("dave@example.com")))
                .doesNotThrowAnyException();

        long count = emailVerificationRepository.findAll().stream()
                .filter(ev -> ev.getUser().getId().equals(user.getId()))
                .count();
        assertThat(count)
                .as("new OTP record must be committed even when email delivery fails")
                .isEqualTo(2);
    }

    // ─── Payload verification tests ───────────────────────────────────────────

    @Test
    void register_resendSuccess_otpEmailPayloadContainsRecipientAndSixDigitCode() {
        MOCK_STATUS.set(200);

        authService.register(new RegisterRequest("eve@example.com", "StrongPass1!", "Eve", null));

        // OTP email is sent synchronously within register() — it is the first captured body.
        assertThat(CAPTURED_BODIES).as("mock Resend server must receive at least one call").isNotEmpty();
        String otpPayload = CAPTURED_BODIES.get(0);

        // Recipient and sender
        assertThat(otpPayload).as("recipient must be in the payload").contains("eve@example.com");
        assertThat(otpPayload).as("sender address must be in the payload").contains("noreply@test.dev");

        // Subject
        assertThat(otpPayload).as("subject must mention verification code").contains("verification code");

        // OTP — a 6-digit numeric code must appear in the HTML body
        assertThat(otpPayload)
                .as("HTML body must contain a 6-digit OTP code")
                .containsPattern("\\b\\d{6}\\b");
    }

    @Test
    void register_resendSuccess_payloadDoesNotContainPasswordOrApiKey() {
        MOCK_STATUS.set(200);

        authService.register(new RegisterRequest("frank@example.com", "S3cr3tPass!", null, null));

        assertThat(CAPTURED_BODIES).isNotEmpty();
        // Check every call captured (OTP email + possibly admin notification)
        for (String body : CAPTURED_BODIES) {
            assertThat(body)
                    .as("password must never appear in any email payload")
                    .doesNotContain("S3cr3tPass!");
            assertThat(body)
                    .as("API key must never appear in any email payload")
                    .doesNotContain("test-key");
        }
    }

    @Test
    void register_resendSuccess_fromFieldMatchesConfiguredSenderAddress() {
        MOCK_STATUS.set(200);

        authService.register(new RegisterRequest("grace@example.com", "StrongPass1!", null, null));

        assertThat(CAPTURED_BODIES).isNotEmpty();
        String otpPayload = CAPTURED_BODIES.get(0);
        // The JSON "from" field must carry the configured sender address.
        assertThat(otpPayload).contains("\"from\"");
        assertThat(otpPayload).contains("noreply@test.dev");
    }

    // ─── Test configuration ───────────────────────────────────────────────────

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        EmbeddingClient fakeEmbeddingClient() {
            return new FakeEmbeddingClient(768);
        }

        @Bean
        @Primary
        LlmClient fakeLlmClient() {
            return new FakeLlmClient("{}");
        }
    }
}
