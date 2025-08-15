package com.revapps.customerapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Customer API Application - REST API for customer management with Azure Service Bus integration
 * 
 * Features:
 * - CRUD operations for customers with PostgreSQL
 * - Event publishing to Azure Service Bus for all customer operations
 * - Managed Identity authentication for Azure services
 * - Comprehensive error handling and logging
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableRetry
public class CustomerApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerApiApplication.class, args);
    }
}
