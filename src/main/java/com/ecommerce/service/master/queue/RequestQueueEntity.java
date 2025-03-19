package com.ecommerce.service.master.queue;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RequestQueueEntity {
    /**
     * The Concurrent Hash Map which contains the reactive sinks (acts like a queue!) per endpoint.
     * It helps formally queuing the requests for processing.
     *
     * Singleton pattern.
     */
    private final Map<String, Sinks.Many<String>> requestQueues = new ConcurrentHashMap<>();

    public Sinks.Many<String> getOrCreateQueue(String endpoint) {
        // If the queue does not exists, initializes a new multicast sink with backpressure handling
        return requestQueues.computeIfAbsent(endpoint, key -> Sinks.many().multicast().onBackpressureBuffer());
    }
}