package com.ecommerce.controller.master;

import com.ecommerce.service.master.GatewayService;
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

@RestController
@RequestMapping("/gateway")
public class GatewayController {

    private final Logger log = LogManager.getLogger(this.getClass());

    @Autowired
    private GatewayService gatewayService;

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

        List<Mono<Map.Entry<String, Map<String, List<?>>>>> responseMonos = requestMap.entrySet()
                .stream()
                .map(entry -> gatewayService.queueAndProcessRequest(entry.getKey(), entry.getValue()))
                .toList();

        return Mono.zip(responseMonos, results -> {
                    Map<String, Map<String, List<?>>> finalResponse = new HashMap<>();

                    for (Object result : results) {
                        @SuppressWarnings("unchecked")
                        Map.Entry<String, Map<String, List<?>>> entry = (Map.Entry<String, Map<String, List<?>>>) result;
                        finalResponse.put(entry.getKey(), entry.getValue());
                    }

                    // Ensure all requested IDs appear in the response, even if null //TODO: do I need it ?
//                    for (Map.Entry<String, List<String>> request : requestMap.entrySet()) {
//                        String endpoint = request.getKey();
//                        List<String> requestedIds = request.getValue();
//                        Map<String, List<?>> responseData = finalResponse.getOrDefault(endpoint, new HashMap<>());
//
//                        for (String id : requestedIds) {
//                            if (!responseData.containsKey(id)) {
//                                responseData.put(id, null); // Add missing IDs with `null`
//                            }
//                        }
//                        finalResponse.put(endpoint, responseData);
//                    }

                    return finalResponse;
                })
                .doOnSuccess(response -> {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    log.info("Finished processing for endpoint(s) {} at {}. Total time: {} ms", nonNullEndpoints, requestParser.getFormattedCurrentTime(), elapsedTime);
                })
                .map(ResponseEntity::ok);
    }
}
