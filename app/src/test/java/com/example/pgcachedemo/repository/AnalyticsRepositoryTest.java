package com.example.pgcachedemo.repository;

import com.example.pgcachedemo.model.CategoryRevenue;
import com.example.pgcachedemo.model.TopProduct;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AnalyticsRepository analyticsRepository;

    @Test
    void revenueByCategory_returnsMappedRows() {
        List<CategoryRevenue> expected = List.of(
                new CategoryRevenue("Electronics", new BigDecimal("1000.00")));
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class)))
                .thenReturn(expected);

        List<CategoryRevenue> result = analyticsRepository.revenueByCategory();

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class));
        assertThat(sqlCaptor.getValue()).contains("GROUP BY c.name");
    }

    @Test
    void topProducts_passesLimit() {
        List<TopProduct> expected = List.of(new TopProduct(1L, "Widget", 42L));
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq(5)))
                .thenReturn(expected);

        List<TopProduct> result = analyticsRepository.topProducts(5);

        assertThat(result).isEqualTo(expected);
        verify(jdbcTemplate).query(any(String.class), any(RowMapper.class), eq(5));
    }
}

