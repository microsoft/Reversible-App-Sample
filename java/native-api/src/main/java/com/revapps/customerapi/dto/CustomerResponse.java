package com.revapps.customerapi.dto;

import java.time.LocalDateTime;

/**
 * DTO for customer response
 */
public record CustomerResponse(
    Integer id,
    String name,
    String email,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
