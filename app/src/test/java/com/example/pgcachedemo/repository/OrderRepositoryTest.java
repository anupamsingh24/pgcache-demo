package com.example.pgcachedemo.repository;

import com.example.pgcachedemo.model.OrderItemDetail;
import com.example.pgcachedemo.model.OrderSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private OrderRepository orderRepository;

    @Test
    void findById_returnsFirstResult_whenPresent() {
        OrderSummary order = new OrderSummary(1L, "completed", OffsetDateTime.now(), 10L, "Alice");
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq(1L)))
                .thenReturn(List.of(order));

        Optional<OrderSummary> result = orderRepository.findById(1L);

        assertThat(result).contains(order);
    }

    @Test
    void findById_returnsEmpty_whenNoRows() {
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq(9L)))
                .thenReturn(List.of());

        Optional<OrderSummary> result = orderRepository.findById(9L);

        assertThat(result).isEmpty();
    }

    @Test
    void findItems_returnsMappedItems() {
        List<OrderItemDetail> items = List.of(
                new OrderItemDetail(1L, 5L, "Widget", 2, new BigDecimal("9.99")));
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq(3L)))
                .thenReturn(items);

        List<OrderItemDetail> result = orderRepository.findItems(3L);

        assertThat(result).isEqualTo(items);
    }
}

