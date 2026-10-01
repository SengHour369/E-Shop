package com.example.eshop.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * Kept from the monolith as-is (not folded into common-lib's APIResponse): its
 * success(message, data) / error(message, code) factory shapes are used pervasively
 * across this domain's controllers/services, and rewriting every call site to
 * APIResponse's (message, status, data) shape would be a large, risky churn for no
 * behavioral benefit while migrating this bounded context.
 */
@Builder
public record ResponseErrorTemplate(
        String message,
        String code,
        @JsonProperty("data")
        Object object
) {
    public static ResponseErrorTemplate success(String message, Object data) {
        return ResponseErrorTemplate.builder()
                .message(message)
                .code("200")
                .object(data)
                .build();
    }

    public static ResponseErrorTemplate error(String message, String code) {
        return ResponseErrorTemplate.builder()
                .message(message)
                .code(code)
                .object(null)
                .build();
    }
}
