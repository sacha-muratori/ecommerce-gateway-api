package com.ecommerce.util;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class GatewayParser {

    public List<String> parseToBigDecimals(List<String> ids) {
        return ids.stream()
                .map(this::validateBigDecimal)
                .toList();
    }

    public List<String> parseAlphanumericCustomerIds(List<String> ids) {
        return ids.stream()
                .map(this::validateCustomerId)
                .toList();
    }

    private String validateBigDecimal(String id) {
        try {
            BigDecimal decimal = new BigDecimal(id);
            decimal = new BigDecimal(decimal.toBigInteger());
            if (decimal.compareTo(BigDecimal.ZERO) > 0 && decimal.scale() == 0) {
                return decimal.toString();
            }
            throw new IllegalArgumentException("For security reasons, only positive integers are allowed.");
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid number format for ID: " + id, e);
        }
    }

    private String validateCustomerId(String id) {
        String sanitizedId = HtmlUtils.htmlEscape(id);
        if (!sanitizedId.matches("[a-zA-Z0-9]+")) {
            throw new IllegalArgumentException("Invalid customer ID: " + id);
        }
        return sanitizedId;
    }

    public String getFormattedCurrentTime() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
    }
}
