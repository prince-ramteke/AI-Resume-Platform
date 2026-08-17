package com.princeramteke.resumeai.auth;

import com.princeramteke.resumeai.auth.dto.*;
import com.princeramteke.resumeai.auth.exception.EmailAlreadyExistsException;
import com.princeramteke.resumeai.auth.exception.InvalidCredentialsException;
import com.princeramteke.resumeai.auth.exception.OtpInvalidException;
import com.princeramteke.resumeai.auth.exception.OtpResendTooSoonException;
import com.princeramteke.resumeai.auth.exception.TooManyOtpAttemptsException;
import com.princeramteke.resumeai.common.exception.GlobalExceptionHandler;
import com.princeramteke.resumeai.config.CorsConfig;
import com.princeramteke.resumeai.config.FeatureFlags;
import com.princeramteke.resumeai.config.JwtConfig;
import com.princeramteke.resumeai.config.RateLimitProperties;
import com.princeramteke.resumeai.config.SecurityConfig;
import com.princeramteke.resumeai.security.AnalysisRateLimitFilter;
import com.princeramteke.resumeai.security.JwtAccessDeniedHandler;
import com.princeramteke.resumeai.security.JwtAuthEntryPoint;
import com.princeramteke.resumeai.security.JwtAuthenticationFilter;
import com.princeramteke.resumeai.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, CorsConfig.class,
        JwtTokenProvider.class, JwtAuthenticationFilter.class,
        JwtAuthEntryPoint.class, JwtAccessDeniedHandler.class,
        GlobalExceptionHandler.class, AnalysisRateLimitFilter.class})
@EnableConfigurationProperties({JwtConfig.class, RateLimitProperties.class, FeatureFlags.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:5173",
        "app.jwt.secret=test-secret-that-is-at-least-32-characters-long-for-hmac",
        "app.jwt.expiry-minutes=60"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private AuthService authService;

    @Test
    void register_validRequest_returns201() throws Exception {
        var response = new RegisterResponse(1L, "prince@example.com", "USER", false);
        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"prince@example.com","password":"StrongPass1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("prince@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        when(authService.register(any())).thenThrow(new EmailAlreadyExistsException("prince@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"prince@example.com","password":"StrongPass1"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"StrongPass1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"prince@example.com","password":"Sh1"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_passwordNoDigit_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"prince@example.com","password":"NoDigitsHere"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_passwordNoLetter_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"prince@example.com","password":"12345678"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_validCredentials_returns200() throws Exception {
        var response = new LoginResponse("jwt.token.here", "Bearer", Instant.parse("2026-08-06T18:00:00Z"), "refresh.token.here", Instant.parse("2026-08-13T18:00:00Z"));
        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"prince@example.com","password":"StrongPass1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt.token.here"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"prince@example.com","password":"WrongPass1"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void me_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_validToken_returns200() throws Exception {
        String token = tokenProvider.generateToken(1L, "prince@example.com", "USER");

        var response = new UserResponse(1L, "prince@example.com", "USER", true, "LOCAL");
        when(authService.getCurrentUser(1L)).thenReturn(response);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("prince@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void refresh_expiredAccessToken_returns200() throws Exception {
        // The blocking bug: an expired access token must NOT prevent refresh from
        // reaching the controller. The token below is expired but well-formed.
        var response = new LoginResponse("new.jwt", "Bearer",
                Instant.parse("2026-08-06T18:00:00Z"), "new.refresh",
                Instant.parse("2026-08-13T18:00:00Z"));
        when(authService.refresh("refresh.token.here")).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                        .header("Authorization", "Bearer " + expiredAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"refresh.token.here"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new.jwt"));

        // Ownership is scoped by the refresh token from the body, not the (expired) principal.
        verify(authService).refresh("refresh.token.here");
    }

    @Test
    void logout_noAccessToken_returns204() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"refresh.token.here"}
                                """))
                .andExpect(status().isNoContent());

        verify(authService).logout("refresh.token.here");
    }

    @Test
    void refresh_invalidRefreshToken_returns401() throws Exception {
        when(authService.refresh(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"stolen-or-unknown"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void protectedEndpoint_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/resumes"))
                .andExpect(status().isUnauthorized());
    }

    // ─── verify-email endpoint ────────────────────────────────────────────────────

    @Test
    void verifyEmail_validRequest_returns200() throws Exception {
        when(authService.verifyEmail(any())).thenReturn(new VerifyEmailResponse("Email verified successfully"));

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"prince@example.com","otp":"123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully"));
    }

    @Test
    void verifyEmail_otpTooShort_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"prince@example.com","otp":"12345"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmail_otpNonNumeric_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"prince@example.com","otp":"ABCDEF"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmail_wrongOtp_returns401() throws Exception {
        when(authService.verifyEmail(any())).thenThrow(new OtpInvalidException());

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"prince@example.com","otp":"999999"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void verifyEmail_lockedAccount_returns423() throws Exception {
        when(authService.verifyEmail(any())).thenThrow(new TooManyOtpAttemptsException());

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"prince@example.com","otp":"999999"}
                                """))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.status").value(423));
    }

    // ─── resend-otp endpoint ──────────────────────────────────────────────────────

    @Test
    void resendOtp_validRequest_returns200() throws Exception {
        when(authService.resendOtp(any()))
                .thenReturn(new ResendOtpResponse("If this email is registered and unverified, a new code has been sent."));

        mockMvc.perform(post("/api/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"prince@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void resendOtp_tooSoon_returns429WithRetryAfterHeader() throws Exception {
        when(authService.resendOtp(any())).thenThrow(new OtpResendTooSoonException(45));

        mockMvc.perform(post("/api/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"prince@example.com"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "45"))
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void resendOtp_blankEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resendOtp_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email"}
                                """))
                .andExpect(status().isBadRequest());
    }

    /** A well-formed but already-expired HS256 token signed with the test secret. */
    private String expiredAccessToken() {
        SecretKey key = Keys.hmacShaKeyFor(
                "test-secret-that-is-at-least-32-characters-long-for-hmac"
                        .getBytes(StandardCharsets.UTF_8));
        Instant expiredAt = Instant.now().minus(1, ChronoUnit.HOURS);
        return Jwts.builder()
                .subject("1")
                .claim("email", "prince@example.com")
                .claim("role", "USER")
                .issuedAt(Date.from(expiredAt.minus(1, ChronoUnit.HOURS)))
                .expiration(Date.from(expiredAt))
                .signWith(key)
                .compact();
    }
}
