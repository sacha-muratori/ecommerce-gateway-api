package com.ecommerce.controller.internal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/internal/order")
public class OrderController {
    private static final Map<String, List<String>> MOCK_ORDERS = new HashMap<>() {{
        put("400000000", List.of("ACTIVE", "a3660234050", "100000000", "555555555"));
        put("411011333", List.of("COMPLETED", "c322229999", "123456789", "588888221"));
        put("420000001", List.of("ACTIVE", "b377889999", "200000001", "666666666"));
        put("430000002", List.of("COMPLETED", "d453217001", "300000002", "777777777"));
        put("440000003", List.of("ACTIVE", "e987654321", "400000003", "888888888"));
        put("450000004", List.of("COMPLETED", "f654987321", "500000004", "999999999"));
        put("460000005", List.of("ACTIVE", "g321456987", "600000005", "111111112"));
        put("470000006", List.of("COMPLETED", "h741852963", "700000006", "222222223"));
        put("480000007", List.of("ACTIVE", "i852369741", "800000007", "333333334"));
        put("490000008", List.of("COMPLETED", "j159753468", "900000008", "444444445"));
        put("500000009", List.of("ACTIVE", "k753159852", "100000009", "555555556"));
        put("510000010", List.of("COMPLETED", "l456789123", "110000010", "666666667"));
        put("520000011", List.of("ACTIVE", "m369258147", "120000011", "777777778"));
        put("530000012", List.of("COMPLETED", "n147852369", "130000012", "888888889"));
        put("540000013", List.of("ACTIVE", "o258147369", "140000013", "999999990"));
        put("550000014", List.of("COMPLETED", "p987321654", "150000014", "111111113"));
        put("560000015", List.of("ACTIVE", "q741963852", "160000015", "222222224"));
        put("570000016", List.of("COMPLETED", "r369147258", "170000016", "333333335"));
        put("580000017", List.of("ACTIVE", "s852741963", "180000017", "444444446"));
        put("590000018", List.of("COMPLETED", "t123456789", "190000018", "555555557"));
    }};

    @GetMapping("/{id}")
    public Mono<ResponseEntity<List<String>>> getOrder(@PathVariable String id) {
        int delay = ThreadLocalRandom.current().nextInt(250, 750); // Random delay between 0.25s and 0.75s
        return Mono.delay(Duration.ofMillis(delay))
                .thenReturn(ResponseEntity.ok(MOCK_ORDERS.getOrDefault(id, null)));
    }
}
