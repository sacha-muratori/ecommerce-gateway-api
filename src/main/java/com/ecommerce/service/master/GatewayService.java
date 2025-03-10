package com.ecommerce.service.master;

import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GatewayService {

    private final Logger log = LogManager.getLogger(this.getClass());
    private final Map<String, Sinks.Many<String>> requestQueues = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Sinks.One<List<?>>>> responseCache = new ConcurrentHashMap<>();

    @Value("${QUEUE_API_WAIT_MAX_TIME}")
    private long queueApiWaitMaxTime; // e.g., 5000 ms

    @Value("${QUEUE_API_PARAM_MAX_CAP}")
    private int queueApiParamMaxCap; // e.g., 5

    @Autowired
    private WebClient webClient;

    @PostConstruct
    private void initQueues() {
        List<String> endpoints = List.of("customer", "product", "inventory", "order", "shipment");

        for (String endpoint : endpoints) {
            requestQueues.put(endpoint, Sinks.many().multicast().onBackpressureBuffer());

            requestQueues.get(endpoint).asFlux()
                .bufferTimeout(queueApiParamMaxCap, Duration.ofMillis(queueApiWaitMaxTime))                             // Collects up to 5 items or waits 5s
                .doOnNext(batch -> logBatchSize(endpoint, batch.size()))
                .flatMap(batch -> fetchBatchData(endpoint, batch))
                .subscribe();
        }
    }

    private void logBatchSize(String endpoint, int size) {
        if (size == queueApiParamMaxCap) {
            log.info("Endpoint [{}]: Emitting a batch of {} requests (queue full).", endpoint, size);
        } else {
            log.info("Endpoint [{}]: Emitting a batch of {} requests (timeout reached).", endpoint, size);
        }
    }

    public Mono<Map.Entry<String, Map<String, List<?>>>> queueAndProcessRequest(String endpoint, List<String> ids) {
        if (ids.isEmpty()) {
            return Mono.just(Map.entry(endpoint, Collections.emptyMap()));
        }

        responseCache.computeIfAbsent(endpoint, k -> new ConcurrentHashMap<>());
        List<Mono<Map.Entry<String, List<?>>>> waitingMonos = ids.stream()
                .map(id -> waitForResponse(endpoint, id))
                .toList();

        return Flux.fromIterable(ids)
                .doOnNext(id -> enqueueRequest(endpoint, id))
                .then(Mono.zip(waitingMonos, results -> buildResponseMap(endpoint, ids, results)));
    }

    private Mono<Map.Entry<String, List<?>>> waitForResponse(String endpoint, String id) {
        Sinks.One<List<?>> sink = Sinks.one();
        responseCache.get(endpoint).put(id, sink);
        return sink.asMono().defaultIfEmpty(Collections.emptyList()).map(data -> Map.entry(id, data));
    }

    private void enqueueRequest(String endpoint, String id) {
        Sinks.Many<String> sink = requestQueues.get(endpoint);
        if (sink == null || sink.tryEmitNext(id).isFailure()) {
            throw new RuntimeException("Failed to queue request for " + endpoint);
        }
    }

    private Map.Entry<String, Map<String, List<?>>> buildResponseMap(String endpoint, List<String> ids, Object[] results) {
        Map<String, List<?>> collectedResults = new HashMap<>();
        for (Object result : results) {
            @SuppressWarnings("unchecked")
            Map.Entry<String, List<?>> entry = (Map.Entry<String, List<?>>) result;
            collectedResults.put(entry.getKey(), entry.getValue());
        }

        // Ensure all requested IDs are present, even if they have null responses
        ids.forEach(id -> collectedResults.putIfAbsent(id, null));

        return Map.entry(endpoint, collectedResults);
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
}
