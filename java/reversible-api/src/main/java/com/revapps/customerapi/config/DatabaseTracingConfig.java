package com.revapps.customerapi.config;

import org.springframework.context.annotation.Configuration;

/**
 * Enhanced database tracing configuration for PostgreSQL with Dapr integration
 * OpenTelemetry auto-instrumentation will handle JDBC tracing automatically
 * Includes Dapr-specific correlation context
 */
@Configuration
public class DatabaseTracingConfig {
    // OpenTelemetry Spring Boot starter handles JDBC instrumentation automatically
    // Dapr sidecar propagates trace context automatically
    // No manual configuration needed for basic database tracing
}
