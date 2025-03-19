package com.ecommerce.service.master;

import com.ecommerce.service.master.batch.BatchRequestHandler;
import com.ecommerce.service.master.queue.RequestQueueEntity;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.List;

@Service
public class GatewayService {

    private final Logger log = LogManager.getLogger(this.getClass());

    @Value("${QUEUE_API_WAIT_MAX_TIME}")
    private long queueApiWaitMaxTime; // e.g., 5000 ms

    @Value("${QUEUE_API_PARAM_MAX_CAP}")
    private int queueApiParamMaxCap; // e.g., 5

    @Autowired
    private WebClient webClient;

    @Autowired
    private RequestQueueEntity queueFactory;

    @Autowired
    private BatchRequestHandler batchProcessor;

    /**
     * This method initializes the request queues for each internal endpoint at application startup.
     * The queues continuously listen for incoming requests, ensuring batches are processed asynchronously
     * up to a maximum size or timeout.
     *
     * When a batch is ready, batch processing is triggered.
     *
     * It uses the RequestQueueFactory to manage shared queues and the BatchProcessor to handle the logic
     * for processing each batch of requests.
     *
     * This Service is used indirectly and reactively by the GatewayController through the RequestQueueProcessor.
     */
    @PostConstruct
    private void initQueues() {
        // Should be dynamically retrieved from either a text file or by reflection reading in the controller internal package
        List<String> endpoints = List.of("customer", "product", "inventory", "order", "shipment");

        // For each internal endpoint, it subscribes to the stream in which the producer emits the value
        for (String endpoint : endpoints) {
            Sinks.Many<String> queue = queueFactory.getOrCreateQueue(endpoint);

            // Defines a set of rules on how to process each batch
            queue
                    .asFlux()                                                                                           // Transforms the push-style sink into a Flux stream
                    .bufferTimeout(queueApiParamMaxCap, Duration.ofMillis(queueApiWaitMaxTime))                         // Collects up to 5 items or waits 5s
                    .doOnNext(batch -> {                                                                                // Every-time an internal endpoint batch is emitted, do the logic

                            // TODO can it be log.debug ?
                            log.info("Endpoint [{}]: Emitting a batch of {} requests ({}).",
                                    endpoint, batch.size(), batch.size() == queueApiParamMaxCap ? "queue full" : "timeout reached");

                            batchProcessor.onBatchReady(endpoint, batch);
                        })
                    .subscribe();                                                                                       // Subscribes to the flux, causing the batch processing to happen asynchronously and continuously
        }
    }
}
