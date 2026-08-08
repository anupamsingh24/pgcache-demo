package com.example.pgcachedemo.dto;

import java.util.List;

public record NewOrderRequest(long customerId, List<NewOrderItemRequest> items) {
}
