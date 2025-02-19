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
    private final Map<String, Map<String, Sinks.One<List<?>>>> responseCache = new ConcurrentHashMap<>();

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
    public Mono<ResponseEntity<Map<String, Map<String, List<?>>>>> getAggregatedData(
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

        List<String> nonNullEndpoints = new ArrayList<>(requestMap.keySet());
        log.info("Starting processing for endpoint(s) {} at {}", nonNullEndpoints, getFormattedCurrentTime());

        long startTime = System.currentTimeMillis();

        List<Mono<Map.Entry<String, Map<String, List<?>>>>> responseMonos = requestMap.entrySet()
                .stream()
                .map(entry -> queueAndProcessRequest(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        return Mono.zip(responseMonos, results -> {
                    Map<String, Map<String, List<?>>> finalResponse = new HashMap<>();
                    for (Object result : results) {
                        @SuppressWarnings("unchecked")
                        Map.Entry<String, Map<String, List<?>>> entry = (Map.Entry<String, Map<String, List<?>>>) result;
                        finalResponse.put(entry.getKey(), entry.getValue());
                    }
                    // Ensure all requested IDs appear in the response, even if null
                    for (Map.Entry<String, List<String>> request : requestMap.entrySet()) {
                        String endpoint = request.getKey();
                        List<String> requestedIds = request.getValue();
                        Map<String, List<?>> responseData = finalResponse.getOrDefault(endpoint, new HashMap<>());

                        for (String id : requestedIds) {
                            if (!responseData.containsKey(id)) {
                                responseData.put(id, null); // Add missing IDs with `null`
                            }
                        }
                        finalResponse.put(endpoint, responseData);
                    }
                    return finalResponse;
                })
                .doOnSuccess(response -> {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    log.info("Finished processing for endpoint(s) {} at {}. Total time: {} ms", nonNullEndpoints, getFormattedCurrentTime(), elapsedTime);
                })
                .map(ResponseEntity::ok);
    }

    private Mono<Map.Entry<String, Map<String, List<?>>>> queueAndProcessRequest(String endpoint, List<String> ids) {
        if (ids.isEmpty()) {
            return Mono.just(Map.entry(endpoint, Collections.emptyMap()));
        }

        responseCache.computeIfAbsent(endpoint, k -> new ConcurrentHashMap<>());

        List<Mono<Map.Entry<String, List<?>>>> waitingMonos = new ArrayList<>();
        for (String id : ids) {
            waitingMonos.add(waitForResponse(endpoint, id));
        }

        return Flux.fromIterable(ids)
                .doOnNext(id -> {
                    Sinks.Many<String> sink = requestQueues.get(endpoint);
                    if (sink == null || sink.tryEmitNext(id).isFailure()) {
                        throw new RuntimeException("Failed to queue request for " + endpoint);
                    }
                })
                .then(Mono.zip(waitingMonos, results -> {
                    Map<String, List<?>> collectedResults = new HashMap<>();
                    for (Object result : results) {
                        @SuppressWarnings("unchecked")
                        Map.Entry<String, List<?>> entry = (Map.Entry<String, List<?>>) result;
                        collectedResults.put(entry.getKey(), entry.getValue());
                    }
                    // Ensure all requested IDs are present, even if they have null responses
                    for (String id : ids) {
                        if (!collectedResults.containsKey(id)) {
                            collectedResults.put(id, null); // Explicitly add missing IDs with null
                        }
                    }

                    return Map.entry(endpoint, collectedResults);
                }));
    }


    private Mono<Map.Entry<String, List<?>>> waitForResponse(String endpoint, String id) {
        Sinks.One<List<?>> sink = Sinks.one();
        responseCache.get(endpoint).put(id, sink);

        return sink.asMono()
                .defaultIfEmpty(Collections.emptyList()) // Ensure missing data returns an empty list
                .map(data -> Map.entry(id, data));
    }

    private Flux<Void> fetchBatchData(String endpoint, List<String> batch) {
        return Flux.fromIterable(batch)
                .flatMap(id -> webClient.get()
                        .uri(uriBuilder -> uriBuilder.path(endpoint + "/" + id).build())
                        .retrieve()
                        .toEntity(new ParameterizedTypeReference<>() {})  // Accepts any type
                        .defaultIfEmpty(ResponseEntity.ok(Collections.emptyList())) // Case if Response Entity is null
                        .flatMap(responseEntity -> {
                            Object body = responseEntity.getBody();
                            List<Object> data;

                            // If the response is a String, wrap it in a List
                            if (body instanceof String) {
                                data = List.of(body);
                            } else if (body instanceof List<?>) {
                                data = (List<Object>) body;
                            } else {
                                data = new ArrayList<>(); // case if Body of Response Entity is null
                            }


                            Sinks.One<List<?>> sink = responseCache.get(endpoint).remove(id);
                            if (sink != null) {
                                sink.tryEmitValue(data);
                            }
                            return Mono.empty();
                        })
                );
    }


    private List<String> parseToBigDecimals(List<String> ids) {
        return ids.stream()
                .map(id -> {
                    try {
                        BigDecimal decimal = new BigDecimal(id);
                        decimal = new BigDecimal(decimal.toBigInteger());
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
        return ids.stream()
                .map(id -> {
                    String sanitizedId = HtmlUtils.htmlEscape(id);
                    if (!sanitizedId.matches("[a-zA-Z0-9]+")) {
                        throw new IllegalArgumentException("Invalid customer ID: " + id);
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
