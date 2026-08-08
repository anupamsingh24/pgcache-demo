package com.example.pgcachedemo.web;

import com.example.pgcachedemo.dto.NewOrderItemRequest;
import com.example.pgcachedemo.dto.NewOrderRequest;
import com.example.pgcachedemo.model.OrderItemDetail;
import com.example.pgcachedemo.model.OrderSummary;
import com.example.pgcachedemo.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderController orderController;

    @Test
    void getById_returnsOk_whenOrderExists() {
        OrderSummary order = new OrderSummary(1L, "completed", OffsetDateTime.now(), 10L, "Alice");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        ResponseEntity<OrderSummary> response = orderController.getById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(order);
    }

    @Test
    void getById_returnsNotFound_whenOrderMissing() {
        when(orderRepository.findById(2L)).thenReturn(Optional.empty());

        ResponseEntity<OrderSummary> response = orderController.getById(2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void getItems_delegatesToRepository() {
        List<OrderItemDetail> items = List.of(
                new OrderItemDetail(1L, 5L, "Widget", 2, new BigDecimal("9.99")));
        when(orderRepository.findItems(1L)).thenReturn(items);

        List<OrderItemDetail> result = orderController.getItems(1L);

        assertThat(result).isEqualTo(items);
    }

    @Test
    void create_returnsCreatedWithLocationAndBody() {
        NewOrderRequest request = new NewOrderRequest(10L,
                List.of(new NewOrderItemRequest(5L, 2, new BigDecimal("9.99"))));
        when(orderRepository.create(10L, request.items())).thenReturn(123L);

        ResponseEntity<Map<String, Long>> response = orderController.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).hasToString("/api/orders/123");
        assertThat(response.getBody()).containsEntry("id", 123L);
        verify(orderRepository).create(10L, request.items());
    }
}

