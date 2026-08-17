package com.princeramteke.resumeai.auth;

import com.princeramteke.resumeai.auth.dto.LoginRequest;
import com.princeramteke.resumeai.auth.dto.RegisterRequest;
import com.princeramteke.resumeai.auth.exception.EmailAlreadyExistsException;
import com.princeramteke.resumeai.auth.exception.EmailNotVerifiedException;
import com.princeramteke.resumeai.auth.exception.InvalidCredentialsException;
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

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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

    // ─── Helpers ──────────────────────────────────────────────────────────────

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
