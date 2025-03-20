package com.ecommerce.service.master.batch;

import com.ecommerce.service.master.cache.ResponseCacheEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Collections;
import java.util.List;

@Service
public class BatchRequestHandler implements BatchEventListener {

    @Autowired
    private WebClient webClient;

    @Autowired
    private ResponseCacheEntity responseCacheManager;

    @Override
    public void onBatchReady(String endpoint, List<String> batch) {
        fetchBatchData(endpoint, batch).subscribe();
    }

    /**
     * This method makes a single HTTP GET request for each ID, processes the response,
     * and emits the data back into a Sinks.One cache for further consumption.
     * It completes each request asynchronously and returns Flux<Void>, meaning it performs asynchronous operations
     * with no expected result, just a signal that the operation has finished.
     *
     * @param endpoint
     * @param batch
     * @return
     */
    private Flux<Void> fetchBatchData(String endpoint, List<String> batch) {
        return Flux.fromIterable(batch)
                .flatMap(id -> webClient.get()
                        .uri(uriBuilder -> uriBuilder.path(endpoint + "/" + id).build())
                        .retrieve()
                        .toEntity(new ParameterizedTypeReference<>() {})
                        .flatMap(responseEntity -> {
                            Object body = responseEntity.getBody();
                            List<Object> data = (body instanceof List<?>) ? (List<Object>) body :
                                    (body != null ? List.of(body) : Collections.emptyList());

                            // Gets (and removes) the particular Sinks.One<List<?>> based on the id as key from the cache
                            Sinks.One<List<?>> sink = responseCacheManager.getCache().get(endpoint).remove(id);
                            if (sink != null) {
                                // Notifies any listeners or consumers that the data for this ID is now available (Gateway Service)
                                sink.tryEmitValue(data);
                            }

                            // Each HTTP GET id process returns an empty Mono to the Flux, a formality since the Flux is not used and is empty
                            return Mono.empty();
                        })
                );
    }
}
