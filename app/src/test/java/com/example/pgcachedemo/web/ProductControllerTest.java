package com.example.pgcachedemo.web;

import com.example.pgcachedemo.dto.PriceUpdateRequest;
import com.example.pgcachedemo.model.Product;
import com.example.pgcachedemo.repository.ProductRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductController productController;

    private Product sampleProduct(long id) {
        return new Product(id, "Widget", "A widget", 3L,
                new BigDecimal("9.99"), 100, OffsetDateTime.now());
    }

    @Test
    void getById_returnsOk_whenProductExists() {
        Product product = sampleProduct(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ResponseEntity<Product> response = productController.getById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(product);
    }

    @Test
    void getById_returnsNotFound_whenProductMissing() {
        when(productRepository.findById(42L)).thenReturn(Optional.empty());

        ResponseEntity<Product> response = productController.getById(42L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void list_usesCategoryLookup_whenCategoryProvided() {
        List<Product> expected = List.of(sampleProduct(1L), sampleProduct(2L));
        when(productRepository.findByCategory(5L, "price", 1, 20)).thenReturn(expected);

        List<Product> result = productController.list(5L, "price", 1, 20);

        assertThat(result).isEqualTo(expected);
        verify(productRepository).findByCategory(5L, "price", 1, 20);
    }

    @Test
    void list_usesPlainList_whenNoCategory() {
        List<Product> expected = List.of(sampleProduct(1L));
        when(productRepository.list("name", 0, 20)).thenReturn(expected);

        List<Product> result = productController.list(null, "name", 0, 20);

        assertThat(result).isEqualTo(expected);
        verify(productRepository).list("name", 0, 20);
    }

    @Test
    void search_delegatesToRepository() {
        List<Product> expected = List.of(sampleProduct(7L));
        when(productRepository.search("wid", 50)).thenReturn(expected);

        List<Product> result = productController.search("wid", 50);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void updatePrice_returnsNoContent_whenRowUpdated() {
        BigDecimal newPrice = new BigDecimal("19.99");
        when(productRepository.updatePrice(1L, newPrice)).thenReturn(1);

        ResponseEntity<Void> response = productController.updatePrice(1L, new PriceUpdateRequest(newPrice));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void updatePrice_returnsNotFound_whenNoRowUpdated() {
        BigDecimal newPrice = new BigDecimal("19.99");
        when(productRepository.updatePrice(99L, newPrice)).thenReturn(0);

        ResponseEntity<Void> response = productController.updatePrice(99L, new PriceUpdateRequest(newPrice));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}

