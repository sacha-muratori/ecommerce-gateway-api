package com.ecommerce.controller.internal.base;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public abstract class AbstractInternalController<T> {

    protected abstract Map<String, T> getMockData();

    protected T transformResponse(T value) {
        return value; // Default: No transformation, override if needed
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<T>> getById(@PathVariable String id) {
        int delay = ThreadLocalRandom.current().nextInt(250, 750); // Random delay between 0.25s and 0.75s
        return Mono.delay(Duration.ofMillis(delay))
                .thenReturn(ResponseEntity.ok(transformResponse(getMockData().get(id))));
    }
}
