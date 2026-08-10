package com.princeramteke.resumeai.config;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the v1.1 observability wiring: with {@code prometheus} exposed, the Micrometer
 * Prometheus registry is active and the scrape endpoint is registered. Uses an in-context
 * runner over the real Boot autoconfigurations — no web server, no database, no Prometheus
 * process. It mirrors {@code application.yml}'s exposure list (health,info,prometheus).
 */
class PrometheusEndpointConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    MetricsAutoConfiguration.class,
                    PrometheusMetricsExportAutoConfiguration.class,
                    EndpointAutoConfiguration.class,
                    WebEndpointAutoConfiguration.class))
            .withPropertyValues("management.endpoints.web.exposure.include=health,info,prometheus");

    @Test
    void prometheusMeterRegistry_isActive() {
        runner.run(context -> assertThat(context).hasSingleBean(PrometheusMeterRegistry.class));
    }

    @Test
    void prometheusScrapeEndpoint_isRegistered() {
        runner.run(context -> assertThat(context).hasSingleBean(PrometheusScrapeEndpoint.class));
    }
}
