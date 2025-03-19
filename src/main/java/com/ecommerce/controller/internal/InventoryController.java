package com.ecommerce.controller.internal;

import com.ecommerce.controller.internal.base.AbstractInternalController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/internal/inventory")
public class InventoryController extends AbstractInternalController<String> {

    private static final Map<String, String> MOCK_INVENTORY = new HashMap<>() {{
        put("222222220", "IN STOCK");
        put("200000001", "NOT AVAILABLE");
        put("300000002", "IN STOCK");
        put("310000003", "NOT AVAILABLE");
        put("330000005", "IN STOCK");
        put("350000007", "NOT AVAILABLE");
        put("360000008", "IN STOCK");
        put("380000010", "IN STOCK");
        put("390000011", "NOT AVAILABLE");
        put("410000013", "IN STOCK");
        put("420000014", "NOT AVAILABLE");
        put("450000017", "IN STOCK");
        put("460000018", "NOT AVAILABLE");
    }};

    @Override
    protected Map<String, String> getMockData() {
        return MOCK_INVENTORY;
    }
}
