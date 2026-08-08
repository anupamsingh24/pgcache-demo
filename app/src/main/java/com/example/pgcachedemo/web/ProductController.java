package com.example.pgcachedemo.web;

import com.example.pgcachedemo.dto.PriceUpdateRequest;
import com.example.pgcachedemo.model.Product;
import com.example.pgcachedemo.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable long id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Product> list(
            @RequestParam(required = false) Long category,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (category != null) {
            return productRepository.findByCategory(category, sort, page, size);
        }
        return productRepository.list(sort, page, size);
    }

    @GetMapping("/search")
    public List<Product> search(@RequestParam String q,
                                 @RequestParam(defaultValue = "50") int limit) {
        return productRepository.search(q, limit);
    }

    @PatchMapping("/{id}/price")
    public ResponseEntity<Void> updatePrice(@PathVariable long id, @RequestBody PriceUpdateRequest request) {
        int updated = productRepository.updatePrice(id, request.price());
        return updated > 0 ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
