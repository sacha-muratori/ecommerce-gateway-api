package com.shippingcompany.controller.master;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/gateway")
public class GatewayController {

    private final WebClient webClient;

    public GatewayController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8080/internal").build();
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAggregatedData(
            @RequestParam(required = false) List<String> customer,
            @RequestParam(required = false) List<String> product,
            @RequestParam(required = false) List<String> inventory,
            @RequestParam(required = false) List<String> order,
            @RequestParam(required = false) List<String> shipment) {

        Map<String, Object> response = new ConcurrentHashMap<>();

        List<Mono<?>> requests = new ArrayList<>();

        if (customer != null) {
            customer.forEach(id -> requests.add(webClient.get()
                    .uri("/internal/customer/" + id)
                    .retrieve()
                    .bodyToMono(List.class)
                    .defaultIfEmpty(null)
            ));
//                    .doOnNext(data -> response.put("customer-" + id, data))));
        }

        if (product != null) {
            product.forEach(id -> requests.add(webClient.get()
                    .uri("/internal/product/" + id)
                    .retrieve()
                    .bodyToMono(List.class)
                    .defaultIfEmpty(null)
            ));
//                    .doOnNext(data -> response.put("product-" + id, data))));
        }

        if (inventory != null) {
            inventory.forEach(id -> requests.add(webClient.get()
                    .uri("/internal/inventory/" + id)
                    .retrieve()
                    .bodyToMono(List.class)
                    .defaultIfEmpty(null)
            ));
//                    .doOnNext(data -> response.put("inventory-" + id, data))));
        }

        if (order != null) {
            order.forEach(id -> requests.add(webClient.get()
                    .uri("/internal/order/" + id)
                    .retrieve()
                    .bodyToMono(List.class)
                    .defaultIfEmpty(null)
            ));
//                    .doOnNext(data -> response.put("order-" + id, data))));
        }

        if (shipment != null) {
            shipment.forEach(id -> requests.add(webClient.get()
                    .uri("/internal/shipment/" + id)
                    .retrieve()
                    .bodyToMono(List.class)
                    .defaultIfEmpty(null)
            ));
//                    .doOnNext(data -> response.put("shipment-" + id, data))));
        }

        return Mono.when(requests).thenReturn(ResponseEntity.ok(response)).block();
    }
}
