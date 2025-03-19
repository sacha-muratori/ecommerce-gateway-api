package com.ecommerce.controller.master;

import com.ecommerce.service.master.queue.RequestQueueProcessor;
import com.ecommerce.util.GatewayParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/gateway")
public class GatewayController {

    private final Logger log = LogManager.getLogger(this.getClass());

    @Autowired
    private RequestQueueProcessor requestHandler;

    @Autowired
    private GatewayParser requestParser;

    @GetMapping
    public Mono<ResponseEntity<Map<String, Map<String, List<?>>>>> getAggregatedData(
            @RequestParam(required = false) List<String> customer,
            @RequestParam(required = false) List<String> product,
            @RequestParam(required = false) List<String> inventory,
            @RequestParam(required = false) List<String> order,
            @RequestParam(required = false) List<String> shipment) {

        Map<String, List<String>> requestMap = new HashMap<>();
        Optional.ofNullable(customer).ifPresent(c -> requestMap.put("customer", requestParser.parseAlphanumericCustomerIds(c)));
        Optional.ofNullable(product).ifPresent(p -> requestMap.put("product", requestParser.parseToBigDecimals(p)));
        Optional.ofNullable(inventory).ifPresent(i -> requestMap.put("inventory", requestParser.parseToBigDecimals(i)));
        Optional.ofNullable(order).ifPresent(o -> requestMap.put("order", requestParser.parseToBigDecimals(o)));
        Optional.ofNullable(shipment).ifPresent(s -> requestMap.put("shipment", requestParser.parseToBigDecimals(s)));

        List<String> nonNullEndpoints = new ArrayList<>(requestMap.keySet());
        log.info("Starting processing for endpoint(s) {} at {}", nonNullEndpoints, requestParser.getFormattedCurrentTime());
        long startTime = System.currentTimeMillis();

        // The List of responses, each being a Mono of a Map is populated with the answer
        List<Mono<Map.Entry<String, Map<String, List<?>>>>> responseMonos =
                requestMap
                        .entrySet()
                        .stream()
                        .map(entry -> requestHandler.queueAndProcessRequest(entry.getKey(), entry.getValue()))
                        .toList();

        // The List of Mono<Map<..>> responses are zipped together in 1 single Mono<ResponseEntity<..>
        return Mono.zip(responseMonos,
                        results ->
                                Arrays.stream(results)
                                // Ensures Mono.zip results are correctly extracted and structured into a Map<String, Map<String, List<?>>>
                                .map(result -> (Map.Entry<String, Map<String, List<?>>>) result)
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                                .doOnSuccess(response -> {
                                    long elapsedTime = System.currentTimeMillis() - startTime;
                                    log.info("Finished processing for endpoint(s) {} at {}. Total time: {} ms", nonNullEndpoints, requestParser.getFormattedCurrentTime(), elapsedTime);
                                })
                                .map(ResponseEntity::ok);
    }
}
