package com.revapps.customerapi.config;

import org.springframework.context.annotation.Configuration;

/**
 * Enhanced database tracing configuration for PostgreSQL
 * OpenTelemetry auto-instrumentation will handle JDBC tracing automatically
 */
@Configuration
public class DatabaseTracingConfig {
    // OpenTelemetry Spring Boot starter handles JDBC instrumentation automatically
    // No manual configuration needed for basic database tracing
}
