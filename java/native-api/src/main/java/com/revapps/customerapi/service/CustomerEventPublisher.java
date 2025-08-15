package com.revapps.customerapi.service;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revapps.customerapi.dto.CustomerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for publishing customer events to Azure Service Bus
 * 
 * Features:
 * - Secure publishing using Managed Identity
 * - Automatic retry with exponential backoff for transient failures
 * - Structured logging for monitoring and troubleshooting
 * - JSON serialization of event payloads
 * - Message properties for routing and filtering
 */
@Service
public class CustomerEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(CustomerEventPublisher.class);
    
    private final ServiceBusSenderClient senderClient;
    private final ObjectMapper objectMapper;

    public CustomerEventPublisher(ServiceBusSenderClient senderClient, 
                                 ObjectMapper objectMapper) {
        this.senderClient = senderClient;
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
     * Publishes an event to Azure Service Bus with retry logic
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
            
            // Create Service Bus message with properties for routing and filtering
            ServiceBusMessage message = new ServiceBusMessage(eventJson)
                .setMessageId(String.format("customer-%s-%d-%d", 
                    event.action().toLowerCase(), 
                    event.customerId(), 
                    System.currentTimeMillis()))
                .setSubject("customer.event")
                .setContentType("application/json");

            // Add custom properties for message routing and filtering
            message.getApplicationProperties().put("eventType", "customer." + event.action().toLowerCase());
            message.getApplicationProperties().put("customerId", event.customerId().toString());
            message.getApplicationProperties().put("action", event.action());
            message.getApplicationProperties().put("timestamp", event.timestamp().toString());

            // Send message
            senderClient.sendMessage(message);
            
            logger.info("Successfully published customer event: action={}, customerId={}, customerName={}", 
                       event.action(), event.customerId(), event.customerName());

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize customer event to JSON: action={}, customerId={}, error={}", 
                        event.action(), event.customerId(), e.getMessage(), e);
            throw new RuntimeException("Failed to serialize event", e);
        } catch (Exception e) {
            logger.error("Failed to publish customer event to Service Bus: action={}, customerId={}, error={}", 
                        event.action(), event.customerId(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }
}
