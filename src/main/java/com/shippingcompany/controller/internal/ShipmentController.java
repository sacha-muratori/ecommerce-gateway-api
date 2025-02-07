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
@RequestMapping("/internal/shipment")
public class ShipmentController {
    private static final Map<String, List<?>> MOCK_SHIPMENTS =  new HashMap<>() {
        {
            put("555555555", List.of("3 Days", 15.50, "DHL", "IN TRANSIT"));
            put("588888221", List.of("2 weeks", 40.99, "UPS", "PENDING"));
        }
    };

    @GetMapping("/{id}")
    public Mono<ResponseEntity<List<?>>> getShipment(@PathVariable String id) {
        int delay = ThreadLocalRandom.current().nextInt(500, 1500); // Random delay between 0.5s and 1.5s
        return Mono.delay(Duration.ofMillis(delay))
                    .thenReturn(ResponseEntity.ok(MOCK_SHIPMENTS.getOrDefault(id, null)));
    }
}
