package com.shippingcompany.controller.internal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/shipment")
public class ShipmentController {
    private static final Map<String, Object[]> MOCK_SHIPMENTS = Map.of(
        "555555555", new Object[]{"3 Days", 15.50, "DHL", "IN TRANSIT"},
        "588888221", new Object[]{"2 weeks", 40.99, "UPS", "PENDING"}
    );

    @GetMapping("/{id}")
    public ResponseEntity<Object[]> getShipment(@PathVariable String id) {
        return ResponseEntity.ok(MOCK_SHIPMENTS.getOrDefault(id, null));
    }
}
