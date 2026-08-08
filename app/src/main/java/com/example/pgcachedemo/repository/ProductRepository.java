package com.example.pgcachedemo.repository;

import com.example.pgcachedemo.model.Product;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ProductRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<Product> PRODUCT_MAPPER = (rs, rowNum) -> new Product(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getLong("category_id"),
            rs.getBigDecimal("price"),
            rs.getInt("stock"),
            rs.getObject("created_at", OffsetDateTime.class)
    );

    public ProductRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Single-table SELECT — the simplest cacheable shape. */
    public Optional<Product> findById(long id) {
        String sql = "SELECT id, name, description, category_id, price, stock, created_at " +
                "FROM products WHERE id = ?";
        return jdbcTemplate.query(sql, PRODUCT_MAPPER, id).stream().findFirst();
    }

    /** Single-table SELECT with ORDER BY + LIMIT/OFFSET. */
    public List<Product> list(String sort, int page, int size) {
        String orderColumn = resolveSortColumn(sort);
        String sql = "SELECT id, name, description, category_id, price, stock, created_at " +
                "FROM products ORDER BY " + orderColumn + " LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, PRODUCT_MAPPER, size, page * size);
    }

    /** Single-table SELECT with WHERE + ORDER BY + LIMIT/OFFSET. */
    public List<Product> findByCategory(long categoryId, String sort, int page, int size) {
        String orderColumn = resolveSortColumn(sort);
        String sql = "SELECT id, name, description, category_id, price, stock, created_at " +
                "FROM products WHERE category_id = ? ORDER BY " + orderColumn + " LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, PRODUCT_MAPPER, categoryId, size, page * size);
    }

    /** ILIKE pattern match. */
    public List<Product> search(String query, int limit) {
        String sql = "SELECT id, name, description, category_id, price, stock, created_at " +
                "FROM products WHERE name ILIKE ? LIMIT ?";
        return jdbcTemplate.query(sql, PRODUCT_MAPPER, "%" + query + "%", limit);
    }

    /** Write — used to trigger and time CDC invalidation on the cached path. */
    public int updatePrice(long id, BigDecimal newPrice) {
        return jdbcTemplate.update("UPDATE products SET price = ? WHERE id = ?", newPrice, id);
    }

    // Only these two hardcoded literals ever reach the query string, so this
    // concatenation cannot be used for SQL injection regardless of the "sort" input.
    private String resolveSortColumn(String sort) {
        return "price".equalsIgnoreCase(sort) ? "price" : "name";
    }
}
