package com.revapps.customerapi.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Properties;

/**
 * Configuration class to load environment variables from .env file
 * This allows for easy configuration of Azure Service Bus and other settings
 */
@Configuration
public class EnvironmentConfig {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentConfig.class);
    
    private final ConfigurableEnvironment environment;

    public EnvironmentConfig(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void loadEnvironmentVariables() {
        try {
            // Log current working directory for debugging
            String currentDir = System.getProperty("user.dir");
            logger.info("Current working directory: {}", currentDir);
            
            // Load .env file if it exists
            Dotenv dotenv = Dotenv.configure()
                    .directory(currentDir)  // Explicitly set directory
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();

            // Convert to Properties and add to Spring Environment
            Properties props = new Properties();
            
            // Load all dotenv entries into properties
            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                
                // Convert environment variable names to Spring property names
                String springProperty = convertToSpringProperty(key);
                props.setProperty(springProperty, value);
                
                logger.info("Loaded environment variable: {} = {} -> property: {}", key, value, springProperty);
            });

            // Add properties to Spring Environment with high precedence
            if (!props.isEmpty()) {
                PropertiesPropertySource propertySource = new PropertiesPropertySource("dotenv", props);
                environment.getPropertySources().addFirst(propertySource);
                logger.info("Successfully loaded {} environment variables from .env file", props.size());
                
                // Log the Azure Service Bus namespace specifically
                String asbNamespace = environment.getProperty("AZURE_SERVICEBUS_NAMESPACE");
                logger.info("Azure Service Bus Namespace from environment: {}", asbNamespace);
            } else {
                logger.warn("No environment variables found in .env file");
            }

        } catch (Exception e) {
            logger.error("Could not load .env file", e);
        }
    }

    /**
     * Convert environment variable names to Spring property names
     * Example: DATABASE_URL -> spring.datasource.url
     */
    private String convertToSpringProperty(String envVar) {
        return switch (envVar) {
            // Keep Azure Service Bus variables as-is for direct substitution in application.properties
            case "AZURE_SERVICEBUS_NAMESPACE", "AZURE_SERVICEBUS_TOPIC_NAME", "AZURE_SERVICEBUS_CONNECTION_TIMEOUT_SECONDS" -> envVar;
            case "DATABASE_URL" -> "spring.datasource.url";
            case "DATABASE_USERNAME" -> "spring.datasource.username";
            case "DATABASE_PASSWORD" -> "spring.datasource.password";
            case "OTEL_SERVICE_NAME" -> "otel.service.name";
            case "OTEL_SERVICE_VERSION" -> "otel.service.version";
            case "OTEL_EXPORTER_OTLP_ENDPOINT" -> "otel.exporter.otlp.endpoint";
            case "SERVER_PORT" -> "server.port";
            default -> envVar.toLowerCase().replace('_', '.');
        };
    }
}
