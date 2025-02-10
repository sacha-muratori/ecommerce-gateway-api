package com.ecommerce.controller.internal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/internal/product")
public class ProductController {
    private static final Map<String, List<?>> MOCK_PRODUCTS =  new HashMap<>() {
        {
            put("100000000", List.of("Laptop 16-inch 64GB RAM", 4000.00, "222222220"));
            put("123456789", List.of("Kitchen-Mix pots, pans and knives", 799.99, "200000001"));
        }
    };

    @GetMapping("/{id}")
    public Mono<ResponseEntity<List<?>>> getProduct(@PathVariable String id) {
        int delay = ThreadLocalRandom.current().nextInt(500, 1500); // Random delay between 0.5s and 1.5s
        return Mono.delay(Duration.ofMillis(delay))
                .thenReturn(ResponseEntity.ok(MOCK_PRODUCTS.getOrDefault(id, null)));
    }
}