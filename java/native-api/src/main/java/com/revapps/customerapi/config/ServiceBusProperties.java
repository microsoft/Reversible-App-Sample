package com.revapps.customerapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Configuration properties for Azure Service Bus
 * Uses connection string authentication for container deployment
 */
@ConfigurationProperties(prefix = "azure.servicebus")
@Validated
public record ServiceBusProperties(
    @NotBlank(message = "Service Bus connection string is required")
    String connectionString,
    
    @NotBlank(message = "Topic name is required")
    String topicName,
    
    // Connection timeout in seconds (default: 30)
    int connectionTimeoutSeconds,
    
    // Retry configuration
    RetryConfig retry
) {
    public ServiceBusProperties {
        // Set default values
        if (connectionTimeoutSeconds <= 0) {
            connectionTimeoutSeconds = 30;
        }
        if (retry == null) {
            retry = new RetryConfig(3, 5000, 30000);
        }
    }

    public record RetryConfig(
        int maxAttempts,
        long initialDelayMs,
        long maxDelayMs
    ) {
        public RetryConfig {
            if (maxAttempts <= 0) maxAttempts = 3;
            if (initialDelayMs <= 0) initialDelayMs = 5000;
            if (maxDelayMs <= 0) maxDelayMs = 30000;
        }
    }
}
