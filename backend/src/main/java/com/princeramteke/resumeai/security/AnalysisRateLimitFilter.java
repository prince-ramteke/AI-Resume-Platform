package com.princeramteke.resumeai.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.princeramteke.resumeai.common.dto.ErrorResponse;
import com.princeramteke.resumeai.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caps how often a single authenticated user can invoke the expensive analysis pipeline
 * ({@code POST /api/analyses} only — reads are unaffected). Registered after
 * {@link JwtAuthenticationFilter} so the caller's userId is already on the SecurityContext, and
 * before the controller/service, so a rejection here never reaches {@code AnalysisService} —
 * the v1.1 M1 result cache and ownership/enumeration checks are untouched.
 *
 * <p>Unauthenticated requests (no valid JWT yet) pass through unconsumed; the existing
 * {@link JwtAuthEntryPoint} 401 path still runs downstream, exactly as before this filter
 * existed.
 */
@Component
public class AnalysisRateLimitFilter extends OncePerRequestFilter {

    private static final String LIMITED_METHOD = "POST";
    private static final String LIMITED_PATH = "/api/analyses";

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, Bucket> buckets = new ConcurrentHashMap<>();

    public AnalysisRateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!isLimitedRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Long userId = currentUserId();
        if (userId == null) {
            // Not authenticated yet: let the security chain's own 401 handling run untouched.
            filterChain.doFilter(request, response);
            return;
        }

        Bucket bucket = buckets.computeIfAbsent(userId, id -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        writeTooManyRequests(response, probe);
    }

    private boolean isLimitedRequest(HttpServletRequest request) {
        return LIMITED_METHOD.equals(request.getMethod()) && LIMITED_PATH.equals(request.getRequestURI());
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return auth.getPrincipal() instanceof Long userId ? userId : null;
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(properties.capacity(),
                Refill.greedy(properties.refillTokens(), properties.refillPeriod()));
        return Bucket.builder().addLimit(limit).build();
    }

    private void writeTooManyRequests(HttpServletResponse response, ConsumptionProbe probe) throws IOException {
        long retryAfterSeconds = Math.max(1,
                (probe.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L);

        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        var error = ErrorResponse.of(429, "Too Many Requests",
                "You've hit the analysis rate limit. Try again in " + retryAfterSeconds + "s.", "unknown");
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
