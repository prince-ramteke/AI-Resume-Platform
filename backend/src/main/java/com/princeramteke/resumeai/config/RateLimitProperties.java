package com.princeramteke.resumeai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/** Per-user token-bucket limits for {@code POST /api/analyses}. See AnalysisRateLimitFilter. */
@ConfigurationProperties(prefix = "app.rate-limit.analysis")
public record RateLimitProperties(
        @DefaultValue("5") int capacity,
        @DefaultValue("5") int refillTokens,
        @DefaultValue("15m") Duration refillPeriod
) {
}
