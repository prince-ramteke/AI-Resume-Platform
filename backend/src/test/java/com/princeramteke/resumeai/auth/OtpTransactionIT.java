package com.princeramteke.resumeai.auth;

import com.princeramteke.resumeai.auth.dto.ResendOtpRequest;
import com.princeramteke.resumeai.auth.dto.VerifyEmailRequest;
import com.princeramteke.resumeai.auth.exception.OtpInvalidException;
import com.princeramteke.resumeai.auth.exception.OtpResendTooSoonException;
import com.princeramteke.resumeai.auth.exception.TooManyOtpAttemptsException;
import com.princeramteke.resumeai.llm.FakeLlmClient;
import com.princeramteke.resumeai.llm.LlmClient;
import com.princeramteke.resumeai.rag.embedding.EmbeddingClient;
import com.princeramteke.resumeai.rag.embedding.FakeEmbeddingClient;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.INTEGER;

/**
 * Integration tests for OTP transaction semantics against a real PostgreSQL (pgvector) database.
 *
 * <p>Specifically verifies that {@code @Transactional(noRollbackFor = {OtpInvalidException.class,
 * TooManyOtpAttemptsException.class})} on {@code AuthService.verifyEmail()} causes failed-attempt
 * increments to be committed even when the method throws — something unit tests with mocked
 * repositories cannot prove.
 *
 * <p>Tests are intentionally NOT {@code @Transactional}: if the test joined the service's
 * transaction, the test-level rollback would swallow both and make {@code noRollbackFor} invisible.
 * Each test calls {@code @BeforeEach} cleanup to keep tests independent.
 *
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                // "none" prevents Ollama/OpenAI/Gemini conditional beans from activating;
                // TestConfig below provides @Primary fakes so the context still has clients.
                "app.embedding.provider=none",
                "app.llm.provider=none",
                "app.jwt.secret=test-secret-that-is-at-least-32-characters-long-for-hmac",
                "app.jwt.expiry-minutes=60",
                "app.feature.email-verification-enabled=true",
                "app.otp.expiry-minutes=10",
                "app.otp.max-attempts=5",
                "app.otp.resend-cooldown-seconds=60"
        })
@Testcontainers
@Import(OtpTransactionIT.TestConfig.class)
class OtpTransactionIT {

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
    }

    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;
    @Autowired EmailVerificationRepository emailVerificationRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        // DELETE FROM users cascades (ON DELETE CASCADE) to email_verifications,
        // oauth_exchange_codes, refresh_tokens, resumes, etc.
        emailVerificationRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    // ─── verifyEmail — transaction rollback semantics ─────────────────────────────

    @Test
    void verifyEmail_wrongOtp_attemptCountCommittedDespiteException() {
        // Given
        User user = persistUser("alice@example.com");
        EmailVerification ev = persistVerification(user, "482931", 10);

        // When — wrong OTP throws OtpInvalidException
        assertThatThrownBy(() -> authService.verifyEmail(
                new VerifyEmailRequest("alice@example.com", "111111")))
                .isInstanceOf(OtpInvalidException.class);

        // Then — attempt_count must be 1 in the real DB.
        // Without noRollbackFor the transaction would roll back and this would be 0.
        EmailVerification reloaded = emailVerificationRepository.findById(ev.getId()).orElseThrow();
        assertThat(reloaded.getAttemptCount())
                .as("attempt_count must persist even though the method threw — noRollbackFor is the guard")
                .isEqualTo(1);
        assertThat(userRepository.findByEmail("alice@example.com").orElseThrow().isEmailVerified())
                .as("user must remain unverified after a wrong OTP").isFalse();
    }

    @Test
    void verifyEmail_eachWrongOtpIncrementsCommittedCounter() {
        // Each of the first 4 failures must commit its increment; the 5th locks.
        User user = persistUser("bob@example.com");
        EmailVerification ev = persistVerification(user, "482931", 10);

        for (int attempt = 1; attempt <= 4; attempt++) {
            assertThatThrownBy(() -> authService.verifyEmail(
                    new VerifyEmailRequest("bob@example.com", "111111")))
                    .isInstanceOf(OtpInvalidException.class);

            int count = emailVerificationRepository.findById(ev.getId()).orElseThrow().getAttemptCount();
            assertThat(count)
                    .as("attempt_count in DB after attempt %d", attempt)
                    .isEqualTo(attempt);
        }
    }

    @Test
    void verifyEmail_maxAttemptsExhausted_recordLockedAndCorrectOtpBlocked() {
        // Given
        User user = persistUser("carol@example.com");
        String correctOtp = "482931";
        EmailVerification ev = persistVerification(user, correctOtp, 10);

        // 4 wrong attempts (each commits its increment)
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> authService.verifyEmail(
                    new VerifyEmailRequest("carol@example.com", "111111")))
                    .isInstanceOf(OtpInvalidException.class);
        }

        // 5th wrong attempt → counter reaches max → TooManyOtpAttemptsException
        assertThatThrownBy(() -> authService.verifyEmail(
                new VerifyEmailRequest("carol@example.com", "111111")))
                .isInstanceOf(TooManyOtpAttemptsException.class);

        assertThat(emailVerificationRepository.findById(ev.getId()).orElseThrow().getAttemptCount())
                .as("attempt_count must be 5 (max) after lockout").isEqualTo(5);

        // Even the CORRECT OTP is now blocked — lock check fires before hash comparison
        assertThatThrownBy(() -> authService.verifyEmail(
                new VerifyEmailRequest("carol@example.com", correctOtp)))
                .as("correct OTP must be rejected when record is locked")
                .isInstanceOf(TooManyOtpAttemptsException.class);

        assertThat(emailVerificationRepository.findById(ev.getId()).orElseThrow().getAttemptCount())
                .as("attempt_count must not increase further after lockout").isEqualTo(5);
    }

    @Test
    void verifyEmail_correctOtp_persistsUsedAtAndEmailVerified() {
        // Given
        User user = persistUser("dave@example.com");
        String otp = "482931";
        EmailVerification ev = persistVerification(user, otp, 10);

        // When — correct OTP
        var response = authService.verifyEmail(new VerifyEmailRequest("dave@example.com", otp));

        // Then — used_at set, user verified
        assertThat(response.message()).contains("verified");

        EmailVerification used = emailVerificationRepository.findById(ev.getId()).orElseThrow();
        assertThat(used.getUsedAt())
                .as("used_at must be set in DB after successful verification").isNotNull();
        assertThat(userRepository.findByEmail("dave@example.com").orElseThrow().isEmailVerified())
                .as("email_verified must be true in DB after successful verification").isTrue();
    }

    @Test
    void verifyEmail_usedVerification_cannotBeReused() {
        // Verify the same OTP twice — second call must fail even with correct OTP.
        User user = persistUser("eve@example.com");
        String otp = "482931";
        persistVerification(user, otp, 10);

        authService.verifyEmail(new VerifyEmailRequest("eve@example.com", otp));

        // Second call: used_at is set so findLatestNotExpiredNotUsedByUserId returns empty.
        assertThatThrownBy(() -> authService.verifyEmail(
                new VerifyEmailRequest("eve@example.com", otp)))
                .as("re-using a consumed OTP must be rejected")
                .isInstanceOf(OtpInvalidException.class);
    }

    // ─── resendOtp ────────────────────────────────────────────────────────────────

    @Test
    void resendOtp_createsNewRecordThatSupersedesOldOtp() {
        User user = persistUser("frank@example.com");
        String oldOtp = "111111";
        EmailVerification oldEv = persistVerification(user, oldOtp, 10);

        // Age the old record so the resend cooldown (60 s) is satisfied.
        ageVerification(oldEv.getId(), 300);

        authService.resendOtp(new ResendOtpRequest("frank@example.com"));

        // Two records must now exist for this user.
        long count = emailVerificationRepository.findAll().stream()
                .filter(v -> v.getUser().getId().equals(user.getId()))
                .count();
        assertThat(count).as("two verification records after resend").isEqualTo(2);

        // The old OTP is superseded: verifyEmail checks only the NEWEST non-expired,
        // non-used record, whose hash will not match the old OTP.
        assertThatThrownBy(() -> authService.verifyEmail(
                new VerifyEmailRequest("frank@example.com", oldOtp)))
                .as("old OTP must be rejected after resend (newest record has a different hash)")
                .isInstanceOf(OtpInvalidException.class);
    }

    @Test
    void resendOtp_newRecordStartsWithZeroAttempts() {
        // User has no prior verification; resend creates the first record from scratch.
        User user = persistUser("grace@example.com");

        authService.resendOtp(new ResendOtpRequest("grace@example.com"));

        EmailVerification ev = emailVerificationRepository
                .findMostRecentByUserId(user.getId()).orElseThrow();
        assertThat(ev.getAttemptCount()).as("new verification record starts at zero attempts").isEqualTo(0);
        assertThat(ev.getUsedAt()).as("new record is not yet used").isNull();
        assertThat(ev.getExpiresAt()).as("expiry is in the future").isAfter(Instant.now());
    }

    @Test
    void resendOtp_cooldownEnforced() {
        // First resend — no prior record, so no cooldown gate.
        User user = persistUser("henry@example.com");
        authService.resendOtp(new ResendOtpRequest("henry@example.com"));

        // Immediate second resend — the record just created is within the 60-second cooldown.
        assertThatThrownBy(() -> authService.resendOtp(new ResendOtpRequest("henry@example.com")))
                .isInstanceOf(OtpResendTooSoonException.class)
                .extracting(e -> ((OtpResendTooSoonException) e).getRetryAfterSeconds())
                .asInstanceOf(INTEGER)
                .as("Retry-After must be between 1 and 60 seconds")
                .isBetween(1, 60);
    }

    @Test
    void resendOtp_unknownEmail_returnsGenericResponseWithoutCreatingRecord() {
        authService.resendOtp(new ResendOtpRequest("ghost@example.com"));

        assertThat(emailVerificationRepository.findAll())
                .as("no verification record must be created for an unknown email").isEmpty();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private User persistUser(String email) {
        return userRepository.save(new User(email, passwordEncoder.encode("TestPass1!")));
    }

    private EmailVerification persistVerification(User user, String otp, int expiryMinutes) {
        Instant expiresAt = Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES);
        return emailVerificationRepository.save(new EmailVerification(user, sha256(otp), expiresAt));
    }

    // Bypasses JPA's updatable=false to make a verification look older for cooldown tests.
    private void ageVerification(Long evId, int secondsAgo) {
        jdbcTemplate.update(
                "UPDATE email_verifications SET created_at = NOW() - (? * INTERVAL '1 second') WHERE id = ?",
                secondsAgo, evId);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            var sb = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

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
