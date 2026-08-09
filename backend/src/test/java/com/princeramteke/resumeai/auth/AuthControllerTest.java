package com.princeramteke.resumeai.auth;

import com.princeramteke.resumeai.auth.dto.*;
import com.princeramteke.resumeai.auth.exception.EmailAlreadyExistsException;
import com.princeramteke.resumeai.auth.exception.InvalidCredentialsException;
import com.princeramteke.resumeai.common.exception.GlobalExceptionHandler;
import com.princeramteke.resumeai.config.CorsConfig;
import com.princeramteke.resumeai.config.JwtConfig;
import com.princeramteke.resumeai.config.RateLimitProperties;
import com.princeramteke.resumeai.config.SecurityConfig;
import com.princeramteke.resumeai.security.AnalysisRateLimitFilter;
import com.princeramteke.resumeai.security.JwtAccessDeniedHandler;
import com.princeramteke.resumeai.security.JwtAuthEntryPoint;
import com.princeramteke.resumeai.security.JwtAuthenticationFilter;
import com.princeramteke.resumeai.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, CorsConfig.class,
        JwtTokenProvider.class, JwtAuthenticationFilter.class,
        JwtAuthEntryPoint.class, JwtAccessDeniedHandler.class,
        GlobalExceptionHandler.class, AnalysisRateLimitFilter.class})
@EnableConfigurationProperties({JwtConfig.class, RateLimitProperties.class})
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
        var response = new RegisterResponse(1L, "prince@example.com", "USER");
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
        var response = new LoginResponse("jwt.token.here", Instant.parse("2026-08-06T18:00:00Z"));
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

        var response = new UserResponse(1L, "prince@example.com", "USER");
        when(authService.getCurrentUser(1L)).thenReturn(response);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("prince@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void protectedEndpoint_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/resumes"))
                .andExpect(status().isUnauthorized());
    }
}
