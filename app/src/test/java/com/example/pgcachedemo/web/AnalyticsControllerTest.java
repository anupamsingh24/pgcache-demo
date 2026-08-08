package com.example.pgcachedemo.web;

import com.example.pgcachedemo.model.CategoryRevenue;
import com.example.pgcachedemo.model.TopProduct;
import com.example.pgcachedemo.repository.AnalyticsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private AnalyticsRepository analyticsRepository;

    @InjectMocks
    private AnalyticsController analyticsController;

    @Test
    void revenueByCategory_delegatesToRepository() {
        List<CategoryRevenue> expected = List.of(
                new CategoryRevenue("Electronics", new BigDecimal("1000.00")),
                new CategoryRevenue("Books", new BigDecimal("250.00")));
        when(analyticsRepository.revenueByCategory()).thenReturn(expected);

        List<CategoryRevenue> result = analyticsController.revenueByCategory();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void topProducts_usesProvidedLimit() {
        List<TopProduct> expected = List.of(new TopProduct(1L, "Widget", 42L));
        when(analyticsRepository.topProducts(5)).thenReturn(expected);

        List<TopProduct> result = analyticsController.topProducts(5);

        assertThat(result).isEqualTo(expected);
        verify(analyticsRepository).topProducts(5);
    }
}

