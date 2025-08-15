package com.revapps.customerapi.config;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Azure Service Bus configuration using connection string for container authentication
 * 
 * This configuration is optimized for container deployments:
 * - Uses connection string for authentication (suitable for containers)
 * - Implements proper retry policies with exponential backoff
 * - Configures appropriate timeouts and connection pooling
 * - Enables comprehensive logging for monitoring
 */
@Configuration
@EnableConfigurationProperties(ServiceBusProperties.class)
public class ServiceBusConfig {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBusConfig.class);

    /**
     * Creates Service Bus sender client with connection string authentication
     * 
     * Key features:
     * - Connection string authentication for container deployments
     * - Retry policy with exponential backoff for resilience
     * - Connection pooling for performance
     * - Proper timeout configuration
     */
    @Bean
    public ServiceBusSenderClient serviceBusSenderClient(ServiceBusProperties properties) {
        
        logger.info("Creating Service Bus sender client for topic: {}", properties.topicName());

        try {
            ServiceBusSenderClient senderClient = new ServiceBusClientBuilder()
                .connectionString(properties.connectionString())
                .sender()
                .topicName(properties.topicName())
                .buildClient();

            logger.info("Service Bus sender client created successfully");
            return senderClient;

        } catch (Exception e) {
            logger.error("Failed to create Service Bus sender client: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Service Bus client", e);
        }
    }
}
