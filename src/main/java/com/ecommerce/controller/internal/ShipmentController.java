package com.ecommerce.controller.internal;

import com.ecommerce.controller.internal.base.AbstractInternalController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/shipment")
public class ShipmentController extends AbstractInternalController<List<?>> {

    private static final Map<String, List<?>> MOCK_SHIPMENTS = new HashMap<>() {{
        put("555555555", List.of("3 Days", 15.50, "DHL", "IN TRANSIT"));
        put("588888221", List.of("2 weeks", 40.99, "UPS", "PENDING"));
        put("666666666", List.of("1 week", 20.00, "FedEx", "SHIPPED"));
        put("777777777", List.of("5 Days", 25.75, "DHL", "IN TRANSIT"));
        put("888888888", List.of("10 Days", 30.50, "USPS", "PENDING"));
        put("999999999", List.of("2 Days", 10.99, "Amazon Logistics", "SHIPPED"));
        put("111111112", List.of("6 Days", 22.99, "DHL", "PENDING"));
        put("222222223", List.of("4 Days", 18.75, "FedEx", "IN TRANSIT"));
        put("333333334", List.of("7 Days", 27.99, "UPS", "PENDING"));
        put("444444445", List.of("8 Days", 32.00, "USPS", "IN TRANSIT"));
        put("555555556", List.of("3 Days", 19.50, "Amazon Logistics", "SHIPPED"));
        put("666666667", List.of("9 Days", 35.00, "DHL", "PENDING"));
        put("777777778", List.of("2 Days", 12.50, "FedEx", "SHIPPED"));
        put("888888889", List.of("5 Days", 23.00, "UPS", "IN TRANSIT"));
        put("999999990", List.of("1 Week", 28.00, "USPS", "PENDING"));
        put("111111113", List.of("4 Days", 16.75, "DHL", "SHIPPED"));
        put("222222224", List.of("6 Days", 24.99, "Amazon Logistics", "IN TRANSIT"));
        put("333333335", List.of("8 Days", 29.50, "FedEx", "PENDING"));
        put("444444446", List.of("10 Days", 33.00, "UPS", "IN TRANSIT"));
        put("555555557", List.of("3 Days", 14.99, "USPS", "SHIPPED"));
    }};


    @Override
    protected Map<String, List<?>> getMockData() {
        return MOCK_SHIPMENTS;
    }
}
