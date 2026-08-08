package com.example.pgcachedemo.model;

import java.math.BigDecimal;

public record OrderItemDetail(
        long id,
        long productId,
        String productName,
        int quantity,
        BigDecimal unitPrice
) {
}
