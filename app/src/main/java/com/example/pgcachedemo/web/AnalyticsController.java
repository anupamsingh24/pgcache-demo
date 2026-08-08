package com.example.pgcachedemo.web;

import com.example.pgcachedemo.model.CategoryRevenue;
import com.example.pgcachedemo.model.TopProduct;
import com.example.pgcachedemo.repository.AnalyticsRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsRepository analyticsRepository;

    public AnalyticsController(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    @GetMapping("/revenue-by-category")
    public List<CategoryRevenue> revenueByCategory() {
        return analyticsRepository.revenueByCategory();
    }

    @GetMapping("/top-products")
    public List<TopProduct> topProducts(@RequestParam(defaultValue = "10") int limit) {
        return analyticsRepository.topProducts(limit);
    }
}
