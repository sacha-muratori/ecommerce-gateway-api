package com.ecommerce.service.master.cache;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ResponseCacheEntity {
    /**
     * The Concurrent Hash Map which contains, per different internal endpoint, the Sinks.One<List<?>> responses.
     * It helps formally containing the responses and provide them asynchronously when needed.
     *
     * Singleton pattern.
     */
    private final Map<String, Map<String, List<Sinks.One<List<?>>>>> responseCache = new ConcurrentHashMap<>();

    public Map<String, Map<String, List<Sinks.One<List<?>>>>> getCache() {
        return responseCache;
    }
}