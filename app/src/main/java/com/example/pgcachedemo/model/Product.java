package com.example.pgcachedemo.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Product(
        long id,
        String name,
        String description,
        long categoryId,
        BigDecimal price,
        int stock,
        OffsetDateTime createdAt
) {
}
