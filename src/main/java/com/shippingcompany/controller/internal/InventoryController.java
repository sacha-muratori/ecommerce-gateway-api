package com.shippingcompany.controller.internal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/inventory")
public class InventoryController {
    private static final Map<String, String> MOCK_INVENTORY = Map.of(
        "222222220", "IN STOCK",
        "200000001", "NOT AVAILABLE",
        "234567890", null
    );

    @GetMapping("/{id}")
    public ResponseEntity<String> getInventory(@PathVariable String id) {
        return ResponseEntity.ok(MOCK_INVENTORY.getOrDefault(id, null));
    }
}
