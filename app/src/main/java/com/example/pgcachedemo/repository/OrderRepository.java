package com.example.pgcachedemo.repository;

import com.example.pgcachedemo.dto.NewOrderItemRequest;
import com.example.pgcachedemo.model.OrderItemDetail;
import com.example.pgcachedemo.model.OrderSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<OrderSummary> ORDER_MAPPER = (rs, rowNum) -> new OrderSummary(
            rs.getLong("id"),
            rs.getString("status"),
            rs.getObject("created_at", OffsetDateTime.class),
            rs.getLong("customer_id"),
            rs.getString("customer_name")
    );

    private static final RowMapper<OrderItemDetail> ITEM_MAPPER = (rs, rowNum) -> new OrderItemDetail(
            rs.getLong("id"),
            rs.getLong("product_id"),
            rs.getString("product_name"),
            rs.getInt("quantity"),
            rs.getBigDecimal("unit_price")
    );

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** INNER JOIN, equality condition. */
    public Optional<OrderSummary> findById(long id) {
        String sql = "SELECT o.id, o.status, o.created_at, o.customer_id, c.name AS customer_name " +
                "FROM orders o INNER JOIN customers c ON o.customer_id = c.id WHERE o.id = ?";
        return jdbcTemplate.query(sql, ORDER_MAPPER, id).stream().findFirst();
    }

    /** INNER JOIN, equality condition. */
    public List<OrderItemDetail> findItems(long orderId) {
        String sql = "SELECT oi.id, oi.product_id, p.name AS product_name, oi.quantity, oi.unit_price " +
                "FROM order_items oi INNER JOIN products p ON oi.product_id = p.id WHERE oi.order_id = ?";
        return jdbcTemplate.query(sql, ITEM_MAPPER, orderId);
    }

    /** Write — order + items, non-cacheable, passes straight through to the origin. */
    @Transactional
    public long create(long customerId, List<NewOrderItemRequest> items) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO orders (customer_id, status) VALUES (?, 'completed')",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, customerId);
            return ps;
        }, keyHolder);

        long orderId = keyHolder.getKey().longValue();

        jdbcTemplate.batchUpdate(
                "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)",
                items,
                items.size(),
                (ps, item) -> {
                    ps.setLong(1, orderId);
                    ps.setLong(2, item.productId());
                    ps.setInt(3, item.quantity());
                    ps.setBigDecimal(4, item.unitPrice());
                });

        return orderId;
    }
}
