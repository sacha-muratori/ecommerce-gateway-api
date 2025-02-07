package com.shippingcompany.controller.internal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/customer")
public class CustomerController {
    @GetMapping("/{id}")
    public ResponseEntity<List<String>> getCustomer(@PathVariable String id) {
        Map<String, List<String>> mockCustomers = Map.of(
            "3660234050", List.of("Mark", "Fabbri", "Seychelles"),
            "377889999", List.of("Hugo", "Erikson", "Luxembourg"),
            "322229999", List.of("Elisabeth", "Cierra", "Mexico")
        );

        return ResponseEntity.ok(mockCustomers.getOrDefault(id, null));
    }
}
