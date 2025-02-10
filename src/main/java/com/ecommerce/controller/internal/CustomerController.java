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
@RequestMapping("/internal/customer")
public class CustomerController {
    private static final Map<String, List<String>> MOCK_CUSTOMERS = new HashMap<>() {
        {
            put("a3660234050", List.of("Mark", "Fabbri", "Seychelles"));
            put("b377889999", List.of("Hugo", "Erikson", "Luxembourg"));
            put("c322229999", List.of("Elisabeth", "Cierra", "Mexico"));
        }
    };

    @GetMapping("/{id}")
    public Mono<ResponseEntity<List<String>>> getCustomer(@PathVariable String id) {
        int delay = ThreadLocalRandom.current().nextInt(250, 750); // Random delay between 0.25s and 0.75s
        return Mono.delay(Duration.ofMillis(delay))
                .thenReturn(ResponseEntity.ok(MOCK_CUSTOMERS.getOrDefault(id, null)));
    }
}
