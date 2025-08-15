package com.revapps.observer.service;

import com.revapps.observer.model.CustomerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class CustomerObserverService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerObserverService.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void processCustomerEvent(String eventId, String eventType, String eventSource, 
                                   CustomerEvent customerEvent) {
        
        // Print formatted event to console 
        printCustomerEvent(eventId, eventType, eventSource, customerEvent);
        
        // Log the processed event
        logger.info("Processed customer event: {} for customer {} ({})", 
                   customerEvent.getAction(), 
                   customerEvent.getCustomerName(), 
                   customerEvent.getCustomerId());
    }

    private void printCustomerEvent(String eventId, String eventType, String eventSource, 
                                  CustomerEvent customerEvent) {
        String timestamp = LocalDateTime.now().format(formatter);
        
        System.out.println("=====================================");
        System.out.println("CUSTOMER DAPR EVENT RECEIVED");
        System.out.println("=====================================");
        System.out.println("Timestamp: " + timestamp);
        System.out.println("Event ID: " + eventId);
        System.out.println("Event Type: " + eventType);
        System.out.println("Event Source: " + eventSource);
        System.out.println("-------------------------------------");
        System.out.println("Customer ID: " + customerEvent.getCustomerId());
        System.out.println("Customer Name: " + customerEvent.getCustomerName());
        System.out.println("Customer Email: " + customerEvent.getCustomerEmail());
        System.out.println("Action: " + customerEvent.getAction());
        System.out.println("=====================================");
        System.out.println();
    }
}
