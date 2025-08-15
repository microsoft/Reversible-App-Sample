package com.revapps.customerapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revapps.customerapi.dto.CustomerEvent;
import io.dapr.client.DaprClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for publishing customer events to Azure Service Bus via Dapr
 * 
 * Features:
 * - Secure publishing using Dapr sidecar with Managed Identity
 * - Automatic retry with exponential backoff for transient failures
 * - Structured logging for monitoring and troubleshooting
 * - JSON serialization of event payloads
 * - Message metadata for routing and filtering
 */
@Service
public class DaprCustomerEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(DaprCustomerEventPublisher.class);
    
    private final DaprClient daprClient;
    private final ObjectMapper objectMapper;
    
    @Value("${dapr.pubsub.name}")
    private String pubsubName;
    
    @Value("${dapr.topic.name}")
    private String topicName;

    public DaprCustomerEventPublisher(DaprClient daprClient, ObjectMapper objectMapper) {
        this.daprClient = daprClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Publishes a customer created event
     */
    public void publishCustomerCreated(Integer customerId, String customerName, String customerEmail) {
        CustomerEvent event = new CustomerEvent(
            "CREATE",
            customerId,
            customerName,
            customerEmail,
            LocalDateTime.now()
        );
        publishEvent(event);
    }

    /**
     * Publishes a customer updated event
     */
    public void publishCustomerUpdated(Integer customerId, String customerName, String customerEmail) {
        CustomerEvent event = new CustomerEvent(
            "UPDATE",
            customerId,
            customerName,
            customerEmail,
            LocalDateTime.now()
        );
        publishEvent(event);
    }

    /**
     * Publishes a customer deleted event
     */
    public void publishCustomerDeleted(Integer customerId, String customerName) {
        CustomerEvent event = new CustomerEvent(
            "DELETE",
            customerId,
            customerName,
            null, // No email for delete events
            LocalDateTime.now()
        );
        publishEvent(event);
    }

    /**
     * Publishes an event to Azure Service Bus via Dapr with retry logic
     * 
     * Retry configuration:
     * - Maximum 3 attempts
     * - Exponential backoff starting at 5 seconds
     * - Maximum delay of 30 seconds
     */
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000, multiplier = 2, maxDelay = 30000)
    )
    private void publishEvent(CustomerEvent event) {
        try {
            // Serialize event to JSON
            String eventJson = objectMapper.writeValueAsString(event);
            
            // Create metadata for message routing and filtering
            Map<String, String> metadata = new HashMap<>();
            metadata.put("eventType", "customer." + event.action().toLowerCase());
            metadata.put("customerId", event.customerId().toString());
            metadata.put("action", event.action());
            metadata.put("timestamp", event.timestamp().toString());
            metadata.put("source", "reversible-customer-api");
            // Add routing key for RabbitMQ topic exchange to match pre-configured binding pattern
            metadata.put("routingKey", "customer." + event.action().toLowerCase());
            
            // Publish via Dapr
            daprClient.publishEvent(pubsubName, topicName, eventJson, metadata).block();
            
            logger.info("Successfully published customer event via Dapr: action={}, customerId={}, customerName={}, pubsub={}, topic={}", 
                       event.action(), event.customerId(), event.customerName(), pubsubName, topicName);

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize customer event to JSON: action={}, customerId={}, error={}", 
                        event.action(), event.customerId(), e.getMessage(), e);
            throw new RuntimeException("Failed to serialize event", e);
        } catch (Exception e) {
            logger.error("Failed to publish customer event via Dapr: action={}, customerId={}, pubsub={}, topic={}, error={}", 
                        event.action(), event.customerId(), pubsubName, topicName, e.getMessage(), e);
            throw new RuntimeException("Failed to publish event via Dapr", e);
        }
    }
}
