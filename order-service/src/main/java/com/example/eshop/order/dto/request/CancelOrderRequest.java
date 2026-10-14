package com.example.eshop.order.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CancelOrderRequest(
    @Pattern(regexp = "CHANGED_MIND|BETTER_PRICE|ORDERED_BY_MISTAKE|SHIPPING_TOO_LONG|OTHER") String reason,
    @Size(max = 500) String details
) {
    public String auditRemark() {
        if (reason == null) return "Cancelled by customer";
        String label = switch (reason) {
            case "CHANGED_MIND" -> "Changed my mind";
            case "BETTER_PRICE" -> "Found a better price";
            case "ORDERED_BY_MISTAKE" -> "Ordered by mistake";
            case "SHIPPING_TOO_LONG" -> "Shipping takes too long";
            case "OTHER" -> "Other";
            default -> throw new IllegalArgumentException("Invalid cancellation reason");
        };
        return label + (details == null || details.isBlank() ? "" : ": " + details.strip());
    }
}
