package com.princeramteke.resumeai.auth;

import com.princeramteke.resumeai.auth.dto.LoginRequest;
import com.princeramteke.resumeai.auth.dto.RegisterRequest;
import com.princeramteke.resumeai.auth.dto.ResendOtpRequest;
import com.princeramteke.resumeai.auth.dto.VerifyEmailRequest;
import com.princeramteke.resumeai.auth.exception.EmailAlreadyExistsException;
import com.princeramteke.resumeai.auth.exception.EmailNotVerifiedException;
import com.princeramteke.resumeai.auth.exception.InvalidCredentialsException;
import com.princeramteke.resumeai.auth.exception.OtpInvalidException;
import com.princeramteke.resumeai.auth.exception.OtpResendTooSoonException;
import com.princeramteke.resumeai.auth.exception.TooManyOtpAttemptsException;
import com.princeramteke.resumeai.config.FeatureFlags;
import com.princeramteke.resumeai.config.OtpConfig;
import com.princeramteke.resumeai.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.InstanceOfAssertFactories.INTEGER;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private EmailVerificationRepository emailVerificationRepository;

    // These are value objects — created directly rather than mocked.
    private static final FeatureFlags FLAGS_OFF = new FeatureFlags(false, false, false);
    private static final FeatureFlags FLAGS_EMAIL_ON = new FeatureFlags(true, false, false);
    private static final OtpConfig OTP_CONFIG = new OtpConfig(10, 5, 60);

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder,
                tokenProvider, 7, FLAGS_OFF, OTP_CONFIG, emailVerificationRepository);
    }

    // ─── Register — flag OFF ──────────────────────────────────────────────────

    @Test
    void register_newEmail_createsUser() {
        var request = new RegisterRequest("prince@example.com", "StrongPass1", null, null);
        when(userRepository.existsByEmail("prince@example.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPass1")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            var saved = new User(u.getEmail(), u.getPasswordHash());
            try {
                var idField = User.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(saved, 1L);
            } catch (Exception ignored) {
            }
            return saved;
        });

        var response = authService.register(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("prince@example.com");
        assertThat(response.role()).isEqualTo("USER");
        // Flag off → no verification needed; OTP record must not be created.
        assertThat(response.emailVerificationRequired()).isFalse();
        verify(emailVerificationRepository, never()).save(any());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$10$hashed");
        assertThat(captor.getValue().isEmailVerified()).isTrue();
    }

    @Test
    void register_existingEmail_throwsConflict() {
        var request = new RegisterRequest("exists@example.com", "StrongPass1", null, null);
        when(userRepository.existsByEmail("exists@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    // ─── Register — flag ON ───────────────────────────────────────────────────

    @Test
    void register_flagOn_createsUnverifiedUserAndOtpRecord() {
        var service = new AuthService(userRepository, refreshTokenRepository, passwordEncoder,
                tokenProvider, 7, FLAGS_EMAIL_ON, OTP_CONFIG, emailVerificationRepository);

        var request = new RegisterRequest("new@example.com", "StrongPass1", "Alice", null);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPass1")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            var saved = new User(u.getEmail(), u.getPasswordHash());
            try {
                var idField = User.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(saved, 2L);
            } catch (Exception ignored) {
            }
            return saved;
        });

        var response = service.register(request);

        assertThat(response.emailVerificationRequired()).isTrue();

        // User is saved with emailVerified=false (remains the default).
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().isEmailVerified()).isFalse();

        // OTP record must be created exactly once.
        ArgumentCaptor<EmailVerification> evCaptor = ArgumentCaptor.forClass(EmailVerification.class);
        verify(emailVerificationRepository).save(evCaptor.capture());
        assertThat(evCaptor.getValue().getOtpHash()).isNotBlank();
        assertThat(evCaptor.getValue().getExpiresAt()).isAfter(Instant.now());
    }

    // ─── Login — flag OFF ─────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsToken() {
        var request = new LoginRequest("prince@example.com", "StrongPass1");
        var user = new User("prince@example.com", "$2a$10$hashed");
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, 5L);
        } catch (Exception ignored) {
        }

        when(userRepository.findByEmail("prince@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPass1", "$2a$10$hashed")).thenReturn(true);
        when(tokenProvider.generateToken(5L, "prince@example.com", "USER")).thenReturn("jwt.token.here");
        when(tokenProvider.getExpiry("jwt.token.here")).thenReturn(Instant.now().plusSeconds(3600));

        var response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("jwt.token.here");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresAt()).isAfter(Instant.now());
    }

    @Test
    void login_wrongEmail_throwsUnauthorized() {
        var request = new LoginRequest("noone@example.com", "pass");
        when(userRepository.findByEmail("noone@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        var request = new LoginRequest("prince@example.com", "wrongpass");
        var user = new User("prince@example.com", "$2a$10$hashed");

        when(userRepository.findByEmail("prince@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "$2a$10$hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // ─── Login — flag ON ──────────────────────────────────────────────────────

    @Test
    void login_flagOn_unverifiedLocalUser_throwsEmailNotVerified() {
        var service = new AuthService(userRepository, refreshTokenRepository, passwordEncoder,
                tokenProvider, 7, FLAGS_EMAIL_ON, OTP_CONFIG, emailVerificationRepository);

        var request = new LoginRequest("unverified@example.com", "StrongPass1");
        var user = new User("unverified@example.com", "$2a$10$hashed");
        // emailVerified defaults to false; authProvider defaults to LOCAL.

        when(userRepository.findByEmail("unverified@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPass1", "$2a$10$hashed")).thenReturn(true);

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(EmailNotVerifiedException.class);
    }

    @Test
    void login_flagOn_verifiedLocalUser_succeeds() {
        var service = new AuthService(userRepository, refreshTokenRepository, passwordEncoder,
                tokenProvider, 7, FLAGS_EMAIL_ON, OTP_CONFIG, emailVerificationRepository);

        var request = new LoginRequest("verified@example.com", "StrongPass1");
        var user = new User("verified@example.com", "$2a$10$hashed");
        user.setEmailVerified(true);
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, 3L);
        } catch (Exception ignored) {
        }

        when(userRepository.findByEmail("verified@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPass1", "$2a$10$hashed")).thenReturn(true);
        when(tokenProvider.generateToken(3L, "verified@example.com", "USER")).thenReturn("jwt");
        when(tokenProvider.getExpiry("jwt")).thenReturn(Instant.now().plusSeconds(3600));

        var response = service.login(request);
        assertThat(response.accessToken()).isEqualTo("jwt");
    }

    // ─── getCurrentUser ───────────────────────────────────────────────────────

    @Test
    void getCurrentUser_existingId_returnsUser() {
        var user = new User("prince@example.com", "$2a$10$hashed");
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, 1L);
        } catch (Exception ignored) {
        }

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        var response = authService.getCurrentUser(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("prince@example.com");
        assertThat(response.role()).isEqualTo("USER");
        assertThat(response.authProvider()).isEqualTo("LOCAL");
    }

    @Test
    void getCurrentUser_missingId_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser(99L))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // ─── Refresh ──────────────────────────────────────────────────────────────

    @Test
    void refresh_validToken_rotatesTokensForTokenOwner() {
        var user = userWithId(5L, "prince@example.com");
        var stored = new RefreshToken(user, "storedhash", "fam-1",
                Instant.now(), Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHashAndNotRevokedAndNotExpired(anyString(), any(Instant.class)))
                .thenReturn(Optional.of(stored));
        when(tokenProvider.generateToken(5L, "prince@example.com", "USER")).thenReturn("new.access.token");
        when(tokenProvider.getExpiry("new.access.token")).thenReturn(Instant.now().plusSeconds(3600));

        var response = authService.refresh("raw-refresh-token");

        assertThat(response.accessToken()).isEqualTo("new.access.token");
        verify(tokenProvider).generateToken(5L, "prince@example.com", "USER");
        assertThat(stored.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(stored);
        verifyNoInteractions(userRepository);
    }

    @Test
    void refresh_unknownOrExpiredToken_throwsUnauthorized() {
        when(refreshTokenRepository.findByTokenHashAndNotRevokedAndNotExpired(anyString(), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("bad-token"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(tokenProvider, never()).generateToken(any(), any(), any());
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    @Test
    void logout_validToken_revokesTokenOwnedByUser() {
        var user = userWithId(7L, "kate@example.com");
        var stored = new RefreshToken(user, "storedhash", "fam-2",
                Instant.now(), Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHashAndNotRevokedAndNotExpired(anyString(), any(Instant.class)))
                .thenReturn(Optional.of(stored));

        authService.logout("raw-refresh-token");

        assertThat(stored.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(stored);
        verifyNoInteractions(userRepository);
    }

    @Test
    void logout_unknownToken_throwsUnauthorized() {
        when(refreshTokenRepository.findByTokenHashAndNotRevokedAndNotExpired(anyString(), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.logout("bad-token"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(refreshTokenRepository, never()).save(any());
    }

    // ─── verifyEmail ─────────────────────────────────────────────────────────────

    @Test
    void verifyEmail_correctOtp_marksVerifiedAndReturnsSuccess() {
        var service = serviceWithEmailFlagOn();
        var user = userWithId(10L, "alice@example.com");
        var otp = "482931";
        var verification = new EmailVerification(user, sha256(otp), Instant.now().plusSeconds(600));

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationRepository.findLatestNotExpiredNotUsedByUserId(any(Long.class), any(Instant.class)))
                .thenReturn(Optional.of(verification));

        var response = service.verifyEmail(new VerifyEmailRequest("alice@example.com", otp));

        assertThat(response.message()).contains("verified");
        assertThat(user.isEmailVerified()).isTrue();
        verify(emailVerificationRepository).save(verification);
        verify(userRepository).save(user);
    }

    @Test
    void verifyEmail_wrongOtp_throwsOtpInvalidAndIncrementsCounter() {
        var service = serviceWithEmailFlagOn();
        var user = userWithId(10L, "alice@example.com");
        var verification = new EmailVerification(user, sha256("111111"), Instant.now().plusSeconds(600));

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationRepository.findLatestNotExpiredNotUsedByUserId(any(Long.class), any(Instant.class)))
                .thenReturn(Optional.of(verification));

        assertThatThrownBy(() -> service.verifyEmail(new VerifyEmailRequest("alice@example.com", "999999")))
                .isInstanceOf(OtpInvalidException.class);

        assertThat(verification.getAttemptCount()).isEqualTo(1);
        verify(emailVerificationRepository).save(verification);
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    void verifyEmail_fifthFailedAttempt_throwsTooManyAttempts() {
        var service = serviceWithEmailFlagOn();
        var user = userWithId(10L, "alice@example.com");
        var verification = new EmailVerification(user, sha256("111111"), Instant.now().plusSeconds(600));
        for (int i = 0; i < 4; i++) verification.incrementAttemptCount();

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationRepository.findLatestNotExpiredNotUsedByUserId(any(Long.class), any(Instant.class)))
                .thenReturn(Optional.of(verification));

        assertThatThrownBy(() -> service.verifyEmail(new VerifyEmailRequest("alice@example.com", "999999")))
                .isInstanceOf(TooManyOtpAttemptsException.class);

        assertThat(verification.getAttemptCount()).isEqualTo(5);
        verify(emailVerificationRepository).save(verification);
    }

    @Test
    void verifyEmail_alreadyLockedVerification_throwsTooManyAttemptsWithoutFurtherIncrement() {
        var service = serviceWithEmailFlagOn();
        var user = userWithId(10L, "alice@example.com");
        var verification = new EmailVerification(user, sha256("111111"), Instant.now().plusSeconds(600));
        for (int i = 0; i < 5; i++) verification.incrementAttemptCount();

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationRepository.findLatestNotExpiredNotUsedByUserId(any(Long.class), any(Instant.class)))
                .thenReturn(Optional.of(verification));

        assertThatThrownBy(() -> service.verifyEmail(new VerifyEmailRequest("alice@example.com", "111111")))
                .isInstanceOf(TooManyOtpAttemptsException.class);

        // Lock check fires before the hash comparison — counter must not increment further.
        assertThat(verification.getAttemptCount()).isEqualTo(5);
        verify(emailVerificationRepository, never()).save(any());
    }

    @Test
    void verifyEmail_noActiveVerification_throwsOtpInvalid() {
        var service = serviceWithEmailFlagOn();
        var user = userWithId(10L, "alice@example.com");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationRepository.findLatestNotExpiredNotUsedByUserId(any(Long.class), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyEmail(new VerifyEmailRequest("alice@example.com", "123456")))
                .isInstanceOf(OtpInvalidException.class);
    }

    @Test
    void verifyEmail_unknownEmail_throwsOtpInvalidForAntiEnumeration() {
        var service = serviceWithEmailFlagOn();
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        // Must throw OtpInvalidException — not a user-existence-leaking 404.
        assertThatThrownBy(() -> service.verifyEmail(new VerifyEmailRequest("ghost@example.com", "123456")))
                .isInstanceOf(OtpInvalidException.class);

        verifyNoInteractions(emailVerificationRepository);
    }

    // ─── resendOtp ────────────────────────────────────────────────────────────────

    @Test
    void resendOtp_validUnverifiedUser_createsNewOtpRecord() {
        var service = serviceWithEmailFlagOn();
        var user = userWithId(11L, "bob@example.com");

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationRepository.findMostRecentByUserId(11L)).thenReturn(Optional.empty());

        var response = service.resendOtp(new ResendOtpRequest("bob@example.com"));

        assertThat(response.message()).contains("registered");
        verify(emailVerificationRepository).save(any(EmailVerification.class));
    }

    @Test
    void resendOtp_unknownEmail_returnsGenericSuccessWithoutRepositoryCall() {
        var service = serviceWithEmailFlagOn();
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        var response = service.resendOtp(new ResendOtpRequest("ghost@example.com"));

        assertThat(response.message()).contains("registered");
        verifyNoInteractions(emailVerificationRepository);
    }

    @Test
    void resendOtp_alreadyVerifiedUser_returnsGenericSuccessForAntiEnumeration() {
        var service = serviceWithEmailFlagOn();
        var user = userWithId(11L, "alice@example.com");
        user.setEmailVerified(true);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        var response = service.resendOtp(new ResendOtpRequest("alice@example.com"));

        assertThat(response.message()).contains("registered");
        verifyNoInteractions(emailVerificationRepository);
    }

    @Test
    void resendOtp_withinCooldown_throwsOtpResendTooSoonWithRetryAfterSeconds() {
        var service = serviceWithEmailFlagOn();
        var user = userWithId(11L, "bob@example.com");
        var recent = new EmailVerification(user, sha256("123456"), Instant.now().plusSeconds(600));
        setCreatedAt(recent, Instant.now().minusSeconds(30)); // 30s ago; cooldown is 60s

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationRepository.findMostRecentByUserId(11L)).thenReturn(Optional.of(recent));

        assertThatThrownBy(() -> service.resendOtp(new ResendOtpRequest("bob@example.com")))
                .isInstanceOf(OtpResendTooSoonException.class)
                .extracting(e -> ((OtpResendTooSoonException) e).getRetryAfterSeconds())
                .asInstanceOf(INTEGER)
                .isBetween(1, 60);
    }

    @Test
    void resendOtp_afterCooldownExpired_createsNewOtpRecord() {
        var service = serviceWithEmailFlagOn();
        var user = userWithId(11L, "bob@example.com");
        var old = new EmailVerification(user, sha256("111111"), Instant.now().plusSeconds(600));
        setCreatedAt(old, Instant.now().minusSeconds(200)); // 200s ago; well past the 60s cooldown

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationRepository.findMostRecentByUserId(11L)).thenReturn(Optional.of(old));

        var response = service.resendOtp(new ResendOtpRequest("bob@example.com"));

        assertThat(response.message()).contains("registered");
        verify(emailVerificationRepository).save(any(EmailVerification.class));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private AuthService serviceWithEmailFlagOn() {
        return new AuthService(userRepository, refreshTokenRepository, passwordEncoder,
                tokenProvider, 7, FLAGS_EMAIL_ON, OTP_CONFIG, emailVerificationRepository);
    }

    private String sha256(String input) {
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

    private void setCreatedAt(EmailVerification ev, Instant instant) {
        try {
            var field = EmailVerification.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(ev, instant);
        } catch (Exception ignored) {
        }
    }

    private User userWithId(Long id, String email) {
        var user = new User(email, "$2a$10$hashed");
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (Exception ignored) {
        }
        return user;
    }
}
