package com.example.pgcachedemo.model;

import java.time.OffsetDateTime;

public record OrderSummary(
        long id,
        String status,
        OffsetDateTime createdAt,
        long customerId,
        String customerName
) {
}
