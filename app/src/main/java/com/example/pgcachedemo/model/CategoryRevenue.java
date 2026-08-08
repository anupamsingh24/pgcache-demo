package com.example.pgcachedemo.model;

import java.math.BigDecimal;

public record CategoryRevenue(
        String categoryName,
        BigDecimal revenue
) {
}
