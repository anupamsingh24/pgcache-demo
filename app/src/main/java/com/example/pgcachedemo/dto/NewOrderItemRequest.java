package com.example.pgcachedemo.dto;

import java.math.BigDecimal;

public record NewOrderItemRequest(long productId, int quantity, BigDecimal unitPrice) {
}
