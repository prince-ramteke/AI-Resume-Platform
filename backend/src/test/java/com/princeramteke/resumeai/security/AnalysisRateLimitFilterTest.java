package com.princeramteke.resumeai.security;

import com.princeramteke.resumeai.analysis.AnalysisController;
import com.princeramteke.resumeai.analysis.AnalysisService;
import com.princeramteke.resumeai.analysis.dto.AnalysisResponse;
import com.princeramteke.resumeai.analysis.dto.AnalysisSummaryResponse;
import com.princeramteke.resumeai.common.exception.GlobalExceptionHandler;
import com.princeramteke.resumeai.config.CorsConfig;
import com.princeramteke.resumeai.config.JwtConfig;
import com.princeramteke.resumeai.config.RateLimitProperties;
import com.princeramteke.resumeai.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises {@link AnalysisRateLimitFilter} through the real security filter chain.
 *
 * <p>Uses its own low capacity via {@code @TestPropertySource} (distinct from
 * {@code AnalysisControllerTest}'s properties) so Spring's test-context cache gives this class a
 * fresh {@code ApplicationContext} — and therefore a fresh, empty bucket map — completely
 * decoupled from every other test class's request count.
 *
 * <p>The filter's bucket map is a singleton keyed by userId, and the Spring context (and that
 * singleton) is cached and reused across every {@code @Test} method in this class regardless of
 * declaration order. Each test therefore mints its own never-before-seen userId in
 * {@code @BeforeEach} rather than reusing a shared constant, so no test can be affected by
 * bucket state another test left behind.
 */
@WebMvcTest(AnalysisController.class)
@Import({SecurityConfig.class, CorsConfig.class,
        JwtTokenProvider.class, JwtAuthenticationFilter.class,
        JwtAuthEntryPoint.class, JwtAccessDeniedHandler.class,
        GlobalExceptionHandler.class, AnalysisRateLimitFilter.class})
@EnableConfigurationProperties({JwtConfig.class, RateLimitProperties.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:5173",
        "app.jwt.secret=test-secret-that-is-at-least-32-characters-long-for-hmac",
        "app.jwt.expiry-minutes=60",
        "app.rate-limit.analysis.capacity=2",
        "app.rate-limit.analysis.refill-tokens=2",
        "app.rate-limit.analysis.refill-period=15m"
})
class AnalysisRateLimitFilterTest {

    // Fresh, never-reused ids per test method — see class Javadoc.
    private static final AtomicLong NEXT_USER_ID = new AtomicLong(9_000_000L);

    private static final Instant NOW = Instant.parse("2026-08-07T10:20:00Z");
    private static final String VALID_BODY = "{\"resumeId\":12,\"jobDescriptionId\":7}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private AnalysisService service;

    private String userAToken;
    private String userBToken;

    @BeforeEach
    void setUp() {
        long userA = NEXT_USER_ID.getAndIncrement();
        long userB = NEXT_USER_ID.getAndIncrement();
        userAToken = tokenProvider.generateToken(userA, "a" + userA + "@example.com", "USER");
        userBToken = tokenProvider.generateToken(userB, "b" + userB + "@example.com", "USER");
        when(service.analyze(any(), any())).thenReturn(sampleResponse());
        when(service.listAnalyses(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new AnalysisSummaryResponse(55L, 78, "Backend Engineer", NOW))));
    }

    private AnalysisResponse sampleResponse() {
        return new AnalysisResponse(55L, 78, "Strong backend match",
                List.of(), List.of(), List.of(), List.of(), List.of(),
                "ollama", 4120, NOW);
    }

    @Test
    void create_underCapacity_returns201() throws Exception {
        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userAToken)
                        .content(VALID_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void create_bucketExhausted_returns429WithRetryAfterAndErrorEnvelope() throws Exception {
        // Capacity is 2 for this test class — the first two requests consume the bucket.
        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userAToken)
                        .content(VALID_BODY))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userAToken)
                        .content(VALID_BODY))
                .andExpect(status().isCreated());

        // Third request in the window is rejected.
        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userAToken)
                        .content(VALID_BODY))
                .andExpect(status().is(429))
                .andExpect(header().exists("Retry-After"))
                .andExpect(result -> {
                    int retryAfter = Integer.parseInt(result.getResponse().getHeader("Retry-After"));
                    org.junit.jupiter.api.Assertions.assertTrue(retryAfter > 0,
                            "Retry-After must be a positive number of seconds");
                })
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void create_perUserIsolation_otherUsersBucketUnaffected() throws Exception {
        // Exhaust user A's bucket (capacity 2).
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/analyses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + userAToken)
                            .content(VALID_BODY))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userAToken)
                        .content(VALID_BODY))
                .andExpect(status().is(429));

        // User B has never made a request; their bucket is untouched.
        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userBToken)
                        .content(VALID_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void unrelatedRoutes_neverRateLimited() throws Exception {
        // Exhaust user A's POST /api/analyses bucket (capacity 2) first.
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/analyses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + userAToken)
                            .content(VALID_BODY))
                    .andExpect(status().isCreated());
        }

        // GET /api/analyses (list) and GET /api/analyses/{id} are untouched by the POST-only
        // limiter, even after the bucket for the same user is empty.
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/analyses")
                            .header("Authorization", "Bearer " + userAToken))
                    .andExpect(status().isOk());
        }

        // An unauthenticated POST still gets the normal 401, not a 429 — the filter
        // never consumes a token for a request with no valid principal.
        mockMvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }
}
