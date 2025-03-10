package com.ecommerce.controller.internal;

import com.ecommerce.controller.internal.base.AbstractInternalController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/product")
public class ProductController extends AbstractInternalController<List<?>> {

    private static final Map<String, List<?>> MOCK_PRODUCTS = new HashMap<>() {{
        put("100000000", List.of("Laptop 16-inch 64GB RAM", 4000.00, "222222220"));
        put("123456789", List.of("Kitchen-Mix pots, pans and knives", 799.99, "200000001"));
        put("200000001", List.of("Gaming Mouse", 59.99, "234567890"));
        put("300000002", List.of("Smartphone 256GB", 1299.99, "222222220"));
        put("400000003", List.of("Noise Cancelling Headphones", 349.99, "200000001"));
        put("500000004", List.of("4K Monitor 32-inch", 799.00, "234567890"));
        put("600000005", List.of("Mechanical Keyboard RGB", 149.99, "222222220"));
        put("700000006", List.of("Fitness Tracker Smartwatch", 199.99, "200000001"));
        put("800000007", List.of("Portable Speaker", 129.50, "234567890"));
        put("900000008", List.of("External SSD 2TB", 249.99, "222222220"));
        put("100000009", List.of("Coffee Machine Premium", 499.00, "200000001"));
        put("110000010", List.of("Cordless Vacuum Cleaner", 349.50, "234567890"));
        put("120000011", List.of("Smart Home Security Camera", 179.99, "222222220"));
        put("130000012", List.of("Bluetooth Earbuds", 129.99, "200000001"));
        put("140000013", List.of("Ergonomic Office Chair", 299.99, "234567890"));
        put("150000014", List.of("Wireless Charging Pad", 39.99, "222222220"));
        put("160000015", List.of("E-Reader 10-inch", 249.00, "200000001"));
        put("170000016", List.of("Mini Projector 1080p", 299.50, "234567890"));
        put("180000017", List.of("Smart Light Bulbs (4-pack)", 49.99, "222222220"));
        put("190000018", List.of("Electric Toothbrush", 89.99, "200000001"));
    }};

    @Override
    protected Map<String, List<?>> getMockData() {
        return MOCK_PRODUCTS;
    }
}
