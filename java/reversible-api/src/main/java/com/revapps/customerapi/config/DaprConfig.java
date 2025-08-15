package com.revapps.customerapi.config;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dapr configuration for Azure Service Bus integration
 * 
 * Configures the Dapr client to connect to the local Dapr sidecar
 * which handles the communication with Azure Service Bus.
 */
@Configuration
public class DaprConfig {

    private static final Logger logger = LoggerFactory.getLogger(DaprConfig.class);

    @Value("${dapr.http.port}")
    private int daprHttpPort;

    @Value("${dapr.grpc.port}")
    private int daprGrpcPort;

    /**
     * Creates and configures the Dapr client
     * 
     * The client connects to the Dapr sidecar which manages:
     * - Service discovery
     * - Pub/Sub messaging with Azure Service Bus
     * - State management
     * - Secret management
     * - Security policies
     */
    @Bean
    public DaprClient daprClient() {
        logger.info("Configuring Dapr client: HTTP port={}, gRPC port={}", daprHttpPort, daprGrpcPort);
        
        try {
            // Configure Dapr client to connect to sidecar
            // The DaprClientBuilder will use environment variables or defaults
            DaprClient client = new DaprClientBuilder().build();
            
            logger.info("Dapr client configured successfully");
            return client;
        } catch (Exception e) {
            logger.error("Failed to configure Dapr client: {}", e.getMessage(), e);
            throw new RuntimeException("Could not initialize Dapr client", e);
        }
    }
}
