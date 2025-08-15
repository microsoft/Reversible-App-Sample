package com.revapps.customerapi.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenTelemetry configuration for sending telemetry data to Aspire Dashboard
 * Uses Spring Boot auto-configuration with properties-based setup
 */
@Configuration
public class TelemetryConfig {

    @Value("${otel.service.name:customer-api-native}")
    private String serviceName;

    @Value("${otel.service.version:1.0.0}")
    private String serviceVersion;

    /**
     * Create a tracer instance for manual instrumentation
     * OpenTelemetry SDK is auto-configured by Spring Boot starter
     */
    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(serviceName, serviceVersion);
    }
}
