package com.example.pgcachedemo.repository;

import com.example.pgcachedemo.model.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ProductRepository productRepository;

    private Product sampleProduct(long id) {
        return new Product(id, "Widget", "A widget", 3L,
                new BigDecimal("9.99"), 100, OffsetDateTime.now());
    }

    @Test
    void findById_returnsFirstResult_whenPresent() {
        Product product = sampleProduct(1L);
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq(1L)))
                .thenReturn(List.of(product));

        Optional<Product> result = productRepository.findById(1L);

        assertThat(result).contains(product);
    }

    @Test
    void findById_returnsEmpty_whenNoRows() {
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq(2L)))
                .thenReturn(List.of());

        Optional<Product> result = productRepository.findById(2L);

        assertThat(result).isEmpty();
    }

    @Test
    void list_sortsByName_forUnknownSort_andComputesOffset() {
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), any(), any()))
                .thenReturn(List.of());

        productRepository.list("unknown", 2, 20);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), eq(20), eq(40));
        assertThat(sqlCaptor.getValue()).contains("ORDER BY name");
    }

    @Test
    void list_sortsByPrice_whenRequested() {
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), any(), any()))
                .thenReturn(List.of());

        productRepository.list("PRICE", 0, 10);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), eq(10), eq(0));
        assertThat(sqlCaptor.getValue()).contains("ORDER BY price");
    }

    @Test
    void findByCategory_passesCategoryAndPaging() {
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), any(), any(), any()))
                .thenReturn(List.of());

        productRepository.findByCategory(7L, "name", 1, 5);

        verify(jdbcTemplate).query(any(String.class), any(RowMapper.class), eq(7L), eq(5), eq(5));
    }

    @Test
    void search_wrapsQueryWithWildcards() {
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), any(), any()))
                .thenReturn(List.of());

        productRepository.search("wid", 25);

        verify(jdbcTemplate).query(any(String.class), any(RowMapper.class), eq("%wid%"), eq(25));
    }

    @Test
    void updatePrice_delegatesToJdbcUpdate() {
        BigDecimal price = new BigDecimal("19.99");
        when(jdbcTemplate.update(any(String.class), eq(price), eq(1L))).thenReturn(1);

        int updated = productRepository.updatePrice(1L, price);

        assertThat(updated).isEqualTo(1);
        verify(jdbcTemplate).update(any(String.class), eq(price), eq(1L));
    }
}

