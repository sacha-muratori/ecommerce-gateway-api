package com.shippingcompany.controller.internal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/order")
public class OrderController {
    private static final Map<String, Object[]> MOCK_ORDERS = Map.of(
        "400000000", new Object[]{"ACTIVE", "3660234050", "100000000", "555555555"},
        "411011333", new Object[]{"COMPLETED", "322229999", "123456789", "588888221"}
    );

    @GetMapping("/{id}")
    public ResponseEntity<Object[]> getOrder(@PathVariable String id) {
        return ResponseEntity.ok(MOCK_ORDERS.getOrDefault(id, null));
    }
}
