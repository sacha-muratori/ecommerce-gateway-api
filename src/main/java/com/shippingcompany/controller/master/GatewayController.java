package com.shippingcompany.controller.master;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/gateway")
public class GatewayController {

    @Autowired
    private WebClient webClient;

    @GetMapping
    public Mono<ResponseEntity<Map<String, ?>>> getAggregatedData(
            @RequestParam(required = false) List<String> customer,  // Alphanumeric
            @RequestParam(required = false) List<String> product,   // BigDecimal as String input
            @RequestParam(required = false) List<String> inventory, // BigDecimal as String input
            @RequestParam(required = false) List<String> order,     // BigDecimal as String input
            @RequestParam(required = false) List<String> shipment)  // BigDecimal as String input
    {
        // The inner maps now hold List<?> values
        Map<String, Map<Object, List<?>>> response = new ConcurrentHashMap<>();

        return Mono.when(
                fetchData("customer", parseAlphanumericCustomerIds(customer), response),
                fetchData("product", parseToBigDecimals(product), response),
                fetchData("inventory", parseToBigDecimals(inventory), response),
                fetchData("order", parseToBigDecimals(order), response),
                fetchData("shipment", parseToBigDecimals(shipment), response)
        ).then(Mono.just(ResponseEntity.ok(response)));
    }

    // Parses input strings into BigDecimal (ensuring positive integers only)
    // and throws an exception with a security-related message if not valid.
    private List<BigDecimal> parseToBigDecimals(List<String> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();

        return ids.stream()
                .map(id -> {
                    try {
                        // Use RoundingMode.DOWN instead of BigDecimal.ROUND_DOWN
                        BigDecimal decimal = new BigDecimal(id);
                        decimal = new BigDecimal(decimal.toBigInteger()); // Truncate the decimal part by converting to BigInteger
                        if (decimal.compareTo(BigDecimal.ZERO) > 0 && decimal.scale() == 0) {
                            return decimal;
                        } else {
                            throw new IllegalArgumentException("For security reasons, only positive integers are allowed.");
                        }
                    } catch (NumberFormatException e) {
                        // Throwing a RuntimeException to trigger HTTP 500 error
                        throw new RuntimeException("For security reasons, invalid number format for ID: " + id, e);
                    }
                })
                .collect(Collectors.toList());
    }

    // Safe parsing of alphanumeric customer IDs with sanitization
    private List<String> parseAlphanumericCustomerIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();

        return ids.stream()
                .map(id -> {
                    // Escape potential HTML or JavaScript content using HtmlUtils
                    String sanitizedId = HtmlUtils.htmlEscape(id);

                    // Validate that the ID only contains alphanumeric characters
                    if (!sanitizedId.matches("[a-zA-Z0-9]+")) {
                        throw new IllegalArgumentException("Invalid customer ID: " + id + " - It must be alphanumeric.");
                    }

                    return sanitizedId;
                })
                .collect(Collectors.toList());
    }

    // General method to fetch data from a given endpoint and add it to the response.
    // If the returned list is empty or null, it stores a null value.
    private <T> Mono<Void> fetchData(String endpoint, List<T> ids, Map<String, Map<Object, List<?>>> response) {
        if (ids == null || ids.isEmpty()) return Mono.empty();

        return Flux.fromIterable(ids)
                .flatMap(id -> webClient.get()
                        .uri("/" + endpoint + "/" + id)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<List<?>>() {})
                        .defaultIfEmpty(Collections.emptyList())
                        .map(data -> {
                            // Using Optional to wrap potentially null data safely
                            String mapKey = endpoint.equals("inventory") ? "inventories" : endpoint + "s";
                            Optional<List<?>> optionalData = Optional.ofNullable((List<?>) data);

                            // Set the listData to null if it's empty, using Optional's orElse
                            List<?> listData = optionalData.filter(list -> !list.isEmpty())
                                    .orElse(null);

                            // Initialize the inner map if not already present and put the data
                            response
                                    .computeIfAbsent(mapKey, k -> new ConcurrentHashMap<>())
                                    .put(id, listData);

                            return listData;
                        }))
                .then();
    }
}
