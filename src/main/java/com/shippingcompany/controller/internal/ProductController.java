package com.shippingcompany.controller.internal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/product")
public class ProductController {
    @GetMapping("/{id}")
    public ResponseEntity<List<Object>> getProduct(@PathVariable String id) {
        Map<String, List<Object>> mockProducts = Map.of(
            "100000000", List.of("Laptop 16-inch 64GB RAM", 4000.00, "222222220"),
            "123456789", List.of("Kitchen-Mix pots, pans and knives", 799.99, "200000001")
        );

        return ResponseEntity.ok(mockProducts.getOrDefault(id, null));
    }
}
