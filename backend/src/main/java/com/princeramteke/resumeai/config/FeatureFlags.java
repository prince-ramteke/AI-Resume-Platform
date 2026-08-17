package com.princeramteke.resumeai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Runtime feature flags for capabilities that can be enabled/disabled independently
 * via environment variables without a code redeploy.
 *
 * All flags default to false so local development works without external dependencies
 * (Resend, Google OAuth). Production enables them selectively via Render env vars.
 */
@ConfigurationProperties(prefix = "app.feature")
public record FeatureFlags(
        @DefaultValue("false") boolean emailVerificationEnabled,
        @DefaultValue("false") boolean googleOauthEnabled,
        @DefaultValue("false") boolean notificationEnabled
) {
}
