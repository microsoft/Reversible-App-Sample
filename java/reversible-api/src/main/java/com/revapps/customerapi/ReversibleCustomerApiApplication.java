package com.revapps.customerapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Reversible Customer API Application - REST API for customer management with Dapr Azure Service Bus integration
 * 
 * Features:
 * - CRUD operations for customers with PostgreSQL
 * - Event publishing to Azure Service Bus using Dapr for all customer operations
 * - Managed Identity authentication through Dapr for Azure services
 * - Comprehensive error handling and logging
 * - Dapr sidecar integration for service-to-service communication
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableRetry
public class ReversibleCustomerApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReversibleCustomerApiApplication.class, args);
    }
}
