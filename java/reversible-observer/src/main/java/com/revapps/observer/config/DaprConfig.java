package com.revapps.observer.config;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class DaprConfig {

    private static final Logger logger = LoggerFactory.getLogger(DaprConfig.class);

    @Value("${app.dapr.pubsub-name}")
    private String pubsubName;

    @Value("${app.dapr.topic-name}")
    private String topicName;
    
    @Value("${DAPR_API_HOST:localhost}")
    private String daprApiHost;
    
    @Value("${DAPR_GRPC_PORT:50002}")
    private String daprGrpcPort;
    
    private DaprClient daprClient;

    @PostConstruct
    public void initialize() {
        try {
            // Validate inputs
            if (daprApiHost == null || daprApiHost.trim().isEmpty()) {
                daprApiHost = "localhost";
                logger.warn("DAPR_API_HOST is null or empty, defaulting to localhost");
            }
            if (daprGrpcPort == null || daprGrpcPort.trim().isEmpty()) {
                daprGrpcPort = "50002";
                logger.warn("DAPR_GRPC_PORT is null or empty, defaulting to 50002");
            }
            
            logger.info("DAPR Configuration:");
            logger.info("  - API Host: {}", daprApiHost);
            logger.info("  - GRPC Port: {}", daprGrpcPort);
            logger.info("  - PubSub Name: {}", pubsubName);
            logger.info("  - Topic Name: {}", topicName);
            
            // Create DaprClient - it will automatically use DAPR_GRPC_ENDPOINT env var if set
            // or fall back to localhost:50001 if not
            this.daprClient = new DaprClientBuilder()
                .build();
            logger.info("DAPR client initialized successfully for pubsub: {} and topic: {}", pubsubName, topicName);
        } catch (Exception e) {
            logger.error("Failed to initialize DAPR client", e);
        }
    }
    
    @PreDestroy
    public void cleanup() {
        if (daprClient != null) {
            try {
                daprClient.close();
                logger.info("DAPR client closed");
            } catch (Exception e) {
                logger.error("Error closing DAPR client", e);
            }
        }
    }
    
    public DaprClient getDaprClient() {
        return daprClient;
    }
}
