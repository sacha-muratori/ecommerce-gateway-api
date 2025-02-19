package com.ecommerce.controller.internal;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
        put("300000002", "IN STOCK");
        put("310000003", "NOT AVAILABLE");
        put("320000004", null);
        put("330000005", "IN STOCK");
        put("340000006", null);
        put("350000007", "NOT AVAILABLE");
        put("360000008", "IN STOCK");
        put("370000009", null);
        put("380000010", "IN STOCK");
        put("390000011", "NOT AVAILABLE");
        put("400000012", null);
        put("410000013", "IN STOCK");
        put("420000014", "NOT AVAILABLE");
        put("430000015", null);
        put("440000016", null);
        put("450000017", "IN STOCK");
        put("460000018", "NOT AVAILABLE");
    }};

    @GetMapping("/{id}")
    public Mono<ResponseEntity<String>> getInventory(@PathVariable String id) {
        int delay = ThreadLocalRandom.current().nextInt(250, 750); // Random delay between 0.25s and 0.75s
        return Mono.delay(Duration.ofMillis(delay))
                .thenReturn(ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(quoteIfNotNull(MOCK_INVENTORY.getOrDefault(id, null))));
    }

    private String quoteIfNotNull(String value) {
        return value != null ? "\"" + value + "\"" : "null";
    }

}
