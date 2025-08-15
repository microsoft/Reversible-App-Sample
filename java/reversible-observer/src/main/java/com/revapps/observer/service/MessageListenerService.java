package com.revapps.observer.service;

import com.revapps.observer.model.CustomerEvent;
import io.dapr.Topic;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class MessageListenerService {

    private static final Logger logger = LoggerFactory.getLogger(MessageListenerService.class);

    @Value("${app.dapr.pubsub-name}")
    private String pubsubName;

    @Value("${app.dapr.topic-name}")
    private String topicName;

    @Autowired
    private CustomerObserverService customerObserverService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void startListening() {
        logger.info("Starting Message Listener Service...");
        logger.info("Message Listener Service started successfully. Listening for messages on topic: {} via pubsub: {}", topicName, pubsubName);
    }

    // DAPR sends CloudEvents as JSON - we need to extract the data field and parse it
    @Topic(name = "customer-events", pubsubName = "customer-pubsub")
    @PostMapping(path = "/customer-events")
    public void processMessage(@RequestBody String message) {
        try {
            logger.info("Received DAPR message: {}", message);
            
            // Parse the CloudEvent JSON
            var cloudEventNode = objectMapper.readTree(message);
            
            // Extract CloudEvent metadata
            String eventId = cloudEventNode.has("id") ? cloudEventNode.get("id").asText() : "unknown";
            String eventType = cloudEventNode.has("type") ? cloudEventNode.get("type").asText() : "unknown";
            String eventSource = cloudEventNode.has("source") ? cloudEventNode.get("source").asText() : "unknown";
            String subject = cloudEventNode.has("subject") ? cloudEventNode.get("subject").asText() : null;
            
            logger.info("Processing CloudEvent - ID: {}, Type: {}, Source: {}, Subject: {}", 
                eventId, eventType, eventSource, subject);
            
            // Get the data field from the CloudEvent
            var dataNode = cloudEventNode.get("data");
            if (dataNode != null) {
                logger.info("Data node type: {}", dataNode.getNodeType());
                logger.info("Data node is text: {}", dataNode.isTextual());
                logger.info("Data node raw value: {}", dataNode);

                // Handle both textual and object dataNode
                String customerEventJson;
                if (dataNode.isTextual()) {
                    customerEventJson = dataNode.asText();
                } else {
                    customerEventJson = dataNode.toString();
                }
                logger.info("Parsing customer event data: {}", customerEventJson);

                try {
                    // Parse the JSON string/object to CustomerEvent object
                    CustomerEvent customerEvent = objectMapper.readValue(customerEventJson, CustomerEvent.class);

                    logger.info("Successfully parsed customer event: Action={}, CustomerId={}, CustomerName={}",
                        customerEvent.getAction(), customerEvent.getCustomerId(), customerEvent.getCustomerName());

                    // Process the event with CloudEvent metadata
                    customerObserverService.processCustomerEvent(
                        eventId,
                        eventType,
                        eventSource,
                        customerEvent
                    );
                } catch (Exception parseException) {
                    logger.error("Failed to parse customer event JSON: {}", parseException.getMessage());
                    logger.error("JSON string/object that failed to parse: '{}'", customerEventJson);
                    throw parseException;
                }

            } else {
                logger.warn("No data found in CloudEvent");
            }
            
        } catch (Exception e) {
            logger.error("Error processing DAPR CloudEvent message: {}", e.getMessage(), e);
        }
    }
}
