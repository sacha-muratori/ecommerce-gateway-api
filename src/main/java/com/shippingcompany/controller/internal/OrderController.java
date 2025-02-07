package com.shippingcompany.controller.internal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/internal/order")
public class OrderController {
    private static final Map<String, List<String>> MOCK_ORDERS =  new HashMap<>() {
        {
            put("400000000", List.of("ACTIVE", "3660234050", "100000000", "555555555"));
            put("411011333", List.of("COMPLETED", "322229999", "123456789", "588888221"));
        }
    };

    @GetMapping("/{id}")
    public Mono<ResponseEntity<List<String>>> getOrder(@PathVariable String id) {
        int delay = ThreadLocalRandom.current().nextInt(500, 1500); // Random delay between 0.5s and 1.5s
        return Mono.delay(Duration.ofMillis(delay))
                .thenReturn(ResponseEntity.ok(MOCK_ORDERS.getOrDefault(id, null)));
    }
}
