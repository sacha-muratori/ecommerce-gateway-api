package com.ecommerce.controller.internal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/internal/inventory")
public class InventoryController {
    private static final Map<String, String> MOCK_INVENTORY = new HashMap<>() {{
        put("222222220", "IN STOCK");
        put("200000001", "NOT AVAILABLE");
        put("234567890", null);
    }};

    @GetMapping("/{id}")
    public Mono<ResponseEntity<String>> getInventory(@PathVariable String id) {
        int delay = ThreadLocalRandom.current().nextInt(500, 1500); // Random delay between 0.5s and 1.5s
        return Mono.delay(Duration.ofMillis(delay))
                .thenReturn(ResponseEntity.ok(MOCK_INVENTORY.getOrDefault(id, null)));
    }
}
