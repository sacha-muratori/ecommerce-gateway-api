package com.ecommerce.service.master.queue;

import com.ecommerce.service.master.cache.ResponseCacheEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RequestQueueProcessor {

    @Autowired
    private RequestQueueEntity requestQueue;

    @Autowired
    private ResponseCacheEntity responseCache;

    /**
     * This service is really important as it sits between the GatewayController and the GatewayService logic.
     *
     * It interacts with the RequestQueueSingleton to enqueue each ID for the respective endpoint and uses the ResponseCacheManager
     * to cache the responses. The responses are asynchronously collected for each ID and, once all responses are received,
     * the final result is returned as a Map.Entry containing the endpoint and the map of IDs with their associated response data.
     *
     * The method `queueAndProcessRequest` handles the entire flow of:
     * - Enqueuing IDs for processing,
     * - Waiting for the responses for each ID,
     * - Building and returning the final response map once all responses are collected.
     *
     * It utilizes a reactive approach via Flux and Mono, ensuring that the operations are performed asynchronously.
     *
     * In short, is both responsible for sending the IDs to the queues and collecting the data once the responses are emitted from the sinks.
     *
     * @param endpoint The internal endpoint for which the request is being processed.
     * @param ids A list of IDs for which the requests are being made.
     * @return A Mono containing the final result as a Map.Entry with the endpoint and the map of responses for each ID.
     */
    public Mono<Map.Entry<String, Map<String, List<?>>>> queueAndProcessRequest(String endpoint, List<String> ids) {
        // If ids are empty, return empty Mono
        if (ids.isEmpty()) {
            return Mono.just(Map.entry(endpoint, Collections.emptyMap()));
        }

        // Needed as hm for endpoint is not initialized on first request processing
        responseCache.getCache().computeIfAbsent(endpoint, k -> new ConcurrentHashMap<>());

        List<Mono<Map.Entry<String, List<?>>>> waitingMonos = ids.stream()
                        .map(id -> waitForResponse(endpoint, id))
                        .toList();

        return Flux.fromIterable(ids)
                .doOnNext(id -> enqueueRequest(endpoint, id))
                .then(Mono.zip(waitingMonos, results -> buildResponseMap(endpoint, ids, results)));
    }

    /**
     * Waits for a response for a specific ID by creating a sink and storing it in the response cache.
     * Returns a Mono that emits the response data or an empty list if no data is received.
     *
     * @param endpoint The endpoint for the request.
     * @param id The ID for the request.
     * @return A Mono with the ID and its response data.
     */
    private Mono<Map.Entry<String, List<?>>> waitForResponse(String endpoint, String id) {
        Sinks.One<List<?>> sink = Sinks.one();
        responseCache.getCache().get(endpoint).put(id, sink);
        return sink.asMono().defaultIfEmpty(Collections.emptyList()).map(data -> Map.entry(id, data));
    }

    /**
     * Enqueues a request by adding the given ID to the appropriate request queue for the endpoint.
     * Throws an exception if the queue is full or fails to add the ID.
     *
     * @param endpoint The endpoint for the request.
     * @param id The ID to enqueue.
     */
    private void enqueueRequest(String endpoint, String id) {
        Sinks.Many<String> queue = requestQueue.getOrCreateQueue(endpoint);
        if (queue == null || queue.tryEmitNext(id).isFailure()) {
            throw new RuntimeException("Failed to queue request for " + endpoint + " with ID: " + id);
        }
    }

    /**
     * Constructs a map of IDs and their results, adding missing IDs with a null value.
     * Returns the map with the endpoint as the key and the results as the value.
     *
     * @param endpoint The endpoint for the results.
     * @param ids The list of IDs.
     * @param results The results for each ID.
     * @return A map of IDs to their results.
     */
    private Map.Entry<String, Map<String, List<?>>> buildResponseMap(String endpoint, List<String> ids, Object[] results) {
        Map<String, List<?>> collectedResults = new HashMap<>();
        for (Object result : results) {
            @SuppressWarnings("unchecked")
            Map.Entry<String, List<?>> entry = (Map.Entry<String, List<?>>) result;
            collectedResults.put(entry.getKey(), entry.getValue());
        }

        ids.forEach(id -> collectedResults.putIfAbsent(id, null));

        return Map.entry(endpoint, collectedResults);
    }

}
