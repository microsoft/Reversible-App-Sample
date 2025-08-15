package com.revapps.customerapi.dto;

import java.time.LocalDateTime;

/**
 * Event payload for customer operations published to Azure Service Bus via Dapr
 */
public record CustomerEvent(
    String action,           // CREATE, UPDATE, DELETE
    Integer customerId,
    String customerName,
    String customerEmail,    // Only for CREATE and UPDATE
    LocalDateTime timestamp
) {}
