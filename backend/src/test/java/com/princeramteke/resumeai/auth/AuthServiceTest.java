package com.princeramteke.resumeai.auth;

import com.princeramteke.resumeai.auth.dto.LoginRequest;
import com.princeramteke.resumeai.auth.dto.RegisterRequest;
import com.princeramteke.resumeai.auth.exception.EmailAlreadyExistsException;
import com.princeramteke.resumeai.auth.exception.InvalidCredentialsException;
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

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider tokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, tokenProvider);
    }

    @Test
    void register_newEmail_createsUser() {
        var request = new RegisterRequest("prince@example.com", "StrongPass1");
        when(userRepository.existsByEmail("prince@example.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPass1")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            var saved = new User(u.getEmail(), u.getPasswordHash());
            // simulate ID assignment via reflection
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

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$10$hashed");
    }

    @Test
    void register_existingEmail_throwsConflict() {
        var request = new RegisterRequest("exists@example.com", "StrongPass1");
        when(userRepository.existsByEmail("exists@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

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
    }

    @Test
    void getCurrentUser_missingId_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser(99L))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
