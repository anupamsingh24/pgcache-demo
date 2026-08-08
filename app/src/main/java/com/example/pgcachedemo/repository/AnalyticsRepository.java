package com.example.pgcachedemo.repository;

import com.example.pgcachedemo.model.CategoryRevenue;
import com.example.pgcachedemo.model.TopProduct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AnalyticsRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<CategoryRevenue> REVENUE_MAPPER = (rs, rowNum) -> new CategoryRevenue(
            rs.getString("category_name"),
            rs.getBigDecimal("revenue")
    );

    private static final RowMapper<TopProduct> TOP_PRODUCT_MAPPER = (rs, rowNum) -> new TopProduct(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getLong("units_sold")
    );

    public AnalyticsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Multi-way JOIN + GROUP BY + SUM — exercises PgCache's materialized-result tier. */
    public List<CategoryRevenue> revenueByCategory() {
        String sql = "SELECT c.name AS category_name, SUM(oi.quantity * oi.unit_price) AS revenue " +
                "FROM order_items oi " +
                "INNER JOIN orders o ON oi.order_id = o.id " +
                "INNER JOIN products p ON oi.product_id = p.id " +
                "INNER JOIN categories c ON p.category_id = c.id " +
                "GROUP BY c.name " +
                "ORDER BY revenue DESC";
        return jdbcTemplate.query(sql, REVENUE_MAPPER);
    }

    /** Window function (RANK) — always materialized regardless of cardinality. */
    public List<TopProduct> topProducts(int limit) {
        String sql = "SELECT id, name, units_sold FROM (" +
                "  SELECT p.id, p.name, SUM(oi.quantity) AS units_sold, " +
                "         RANK() OVER (ORDER BY SUM(oi.quantity) DESC) AS rnk " +
                "  FROM order_items oi INNER JOIN products p ON oi.product_id = p.id " +
                "  GROUP BY p.id, p.name" +
                ") ranked WHERE rnk <= ? ORDER BY rnk";
        return jdbcTemplate.query(sql, TOP_PRODUCT_MAPPER, limit);
    }
}
