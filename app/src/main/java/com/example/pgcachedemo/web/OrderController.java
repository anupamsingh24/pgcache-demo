package com.example.pgcachedemo.web;

import com.example.pgcachedemo.dto.NewOrderRequest;
import com.example.pgcachedemo.model.OrderItemDetail;
import com.example.pgcachedemo.model.OrderSummary;
import com.example.pgcachedemo.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderSummary> getById(@PathVariable long id) {
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/items")
    public List<OrderItemDetail> getItems(@PathVariable long id) {
        return orderRepository.findItems(id);
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> create(@RequestBody NewOrderRequest request) {
        long orderId = orderRepository.create(request.customerId(), request.items());
        return ResponseEntity.created(URI.create("/api/orders/" + orderId))
                .body(Map.of("id", orderId));
    }
}
