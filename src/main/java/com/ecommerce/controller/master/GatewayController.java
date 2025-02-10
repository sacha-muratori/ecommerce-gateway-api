package com.ecommerce.controller.master;

import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.HtmlUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/gateway")
public class GatewayController {

    @Value("${QUEUE_API_WAIT_MAX_TIME}")
    private long queueApiWaitMaxTime; // e.g., 5000 ms

    @Value("${QUEUE_API_PARAM_MAX_CAP}")
    private int queueApiParamMaxCap; // e.g., 5

    @Autowired
    private WebClient webClient;

    private final Logger log = LogManager.getLogger(this.getClass());
    private final Map<String, Sinks.Many<String>> requestQueues = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Optional<List<?>>>> responseCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        List<String> endpoints = List.of("customer", "product", "inventory", "order", "shipment");

        for (String endpoint : endpoints) {
            requestQueues.put(endpoint, Sinks.many().multicast().onBackpressureBuffer());

            requestQueues.get(endpoint).asFlux()
                    .bufferTimeout(queueApiParamMaxCap, Duration.ofMillis(queueApiWaitMaxTime))  // Collects up to 5 items or waits 5s
                    .doOnNext(batch -> {
                        if (batch.size() == queueApiParamMaxCap) {
                            log.info("Endpoint [{}]: Emitting a batch of {} requests because the queue was full.",
                                    endpoint, batch.size());
                        } else {
                            log.info("Endpoint [{}]: Emitting a batch of {} requests after waiting 5 seconds.",
                                    endpoint, batch.size());
                        }
                    })
                    .flatMap(batch -> fetchBatchData(endpoint, batch))  // Fetch each batch
                    .subscribe();
        }
    }

    @GetMapping
    public Mono<ResponseEntity<Map<String, Map<String, Optional<List<?>>>>>> getAggregatedData(
            @RequestParam(required = false) List<String> customer,
            @RequestParam(required = false) List<String> product,
            @RequestParam(required = false) List<String> inventory,
            @RequestParam(required = false) List<String> order,
            @RequestParam(required = false) List<String> shipment) {

        Map<String, List<String>> requestMap = new HashMap<>();
        if (customer != null) requestMap.put("customer", parseAlphanumericCustomerIds(customer));
        if (product != null) requestMap.put("product", parseToBigDecimals(product));
        if (inventory != null) requestMap.put("inventory", parseToBigDecimals(inventory));
        if (order != null) requestMap.put("order", parseToBigDecimals(order));
        if (shipment != null) requestMap.put("shipment", parseToBigDecimals(shipment));

        // Log start time
        List<String> nonNullEndpoints = requestMap.keySet().stream().collect(Collectors.toList());
        log.info("Starting processing for endpoint(s) {} at {}", nonNullEndpoints, getFormattedCurrentTime());

        // Get the start time at the beginning of the request
        long startTime = System.currentTimeMillis();

        // Log the processing for each endpoint
        return Flux.fromIterable(requestMap.entrySet())
                .flatMap(entry -> processRequest(entry.getKey(), entry.getValue())
                        .doOnTerminate(() -> {
                            // Log the finish time for each endpoint after processing
                            long elapsedTime = System.currentTimeMillis() - startTime;
                            log.info("Finished processing for endpoint(s) {} at {}. Total time: {} ms", entry.getKey(), getFormattedCurrentTime(), elapsedTime);
                        }))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .map(ResponseEntity::ok); // Return the final response
    }

    private Mono<Map.Entry<String, Map<String, Optional<List<?>>>>> processRequest(String endpoint, List<String> ids) {
        if (ids.isEmpty()) {
            return Mono.just(Map.entry(endpoint, Collections.emptyMap()));
        }

        responseCache.computeIfAbsent(endpoint, k -> new ConcurrentHashMap<>());

        return Flux.fromIterable(ids)
                .doOnNext(id -> {
                    Sinks.Many<String> sink = requestQueues.get(endpoint);
                    if (sink == null || sink.tryEmitNext(id).isFailure()) {
                        throw new RuntimeException("Failed to queue request for " + endpoint);
                    }
                })
                .flatMap(id -> waitForResponse(endpoint, id)) // Wait for response
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .map(data -> Map.entry(endpoint, data));
    }

    private Mono<Map.Entry<String, Optional<List<?>>>> waitForResponse(String endpoint, String id) {
        return Mono.create(sink -> {
            responseCache.get(endpoint).put(id, Optional.empty());

            Flux.interval(Duration.ofMillis(100))
                    .take(Duration.ofSeconds(10)) // Wait up to 10 seconds
                    .map(i -> responseCache.get(endpoint).get(id))
                    .filter(Optional::isPresent)
                    .next()
                    .switchIfEmpty(Mono.just(Optional.empty())) // Fallback: if nothing is emitted after 10s, use Optional.empty()
                    .map(data -> Map.entry(id, data))
                    .subscribe(sink::success, sink::error);
        });
    }

    private Flux<Void> fetchBatchData(String endpoint, List<String> batch) {
        return Flux.fromIterable(batch)
                .flatMap(id -> webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(endpoint + "/" + id)
                                .build()
                        )
                        .retrieve()
                        .toEntity(new ParameterizedTypeReference<List<?>>() {})  // Use toEntity to get ResponseEntity
                        .defaultIfEmpty(ResponseEntity.ok(Collections.emptyList()))  // Default to empty List if not found
                        .flatMap(responseEntity -> {
                            List<?> data = responseEntity.getBody();  // Extract the List<?> from ResponseEntity
                            // Now you can use 'data' (List<?>) as required
                            responseCache.get(endpoint).put(id, Optional.ofNullable(data));
                            return Mono.empty();  // Return Mono.empty() as the return type is Flux<Void>
                        })
                );
    }

    private List<String> parseToBigDecimals(List<String> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();

        return ids.stream()
                .map(id -> {
                    try {
                        BigDecimal decimal = new BigDecimal(id);
                        decimal = new BigDecimal(decimal.toBigInteger()); // Remove decimals
                        if (decimal.compareTo(BigDecimal.ZERO) > 0 && decimal.scale() == 0) {
                            return decimal.toString();
                        } else {
                            throw new IllegalArgumentException("For security reasons, only positive integers are allowed.");
                        }
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("For security reasons, invalid number format for ID: " + id, e);
                    }
                })
                .collect(Collectors.toList());
    }

    private List<String> parseAlphanumericCustomerIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();

        return ids.stream()
                .map(id -> {
                    String sanitizedId = HtmlUtils.htmlEscape(id);
                    if (!sanitizedId.matches("[a-zA-Z0-9]+")) {
                        throw new IllegalArgumentException("Invalid customer ID: " + id + " - It must be alphanumeric.");
                    }
                    return sanitizedId;
                })
                .collect(Collectors.toList());
    }

    private String getFormattedCurrentTime() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
    }
}
