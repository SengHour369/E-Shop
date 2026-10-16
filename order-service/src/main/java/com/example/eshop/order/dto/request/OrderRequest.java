package com.example.eshop.order.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class OrderRequest {

    @NotNull(message = "Shipping address is required")
    @JsonProperty("address_id")
    private Long addressId;

    @NotNull(message = "Payment method is required")
    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("currency")
    private String currency;
    @JsonProperty("checkout_key")
    @jakarta.validation.constraints.Size(max = 100)
    private String checkoutKey;
    @JsonProperty("expected_total")
    @jakarta.validation.constraints.DecimalMin("0")
    private java.math.BigDecimal expectedTotal;
    @JsonProperty("expected_cart_fingerprint")
    @jakarta.validation.constraints.Pattern(regexp="[a-f0-9]{64}")
    private String expectedCartFingerprint;
    public record ReviewedLine(@jakarta.validation.constraints.Positive Long skuId, @jakarta.validation.constraints.Positive Long quantity) {}
    @JsonProperty("reviewed_items") @jakarta.validation.Valid @jakarta.validation.constraints.Size(max=1000)
    private java.util.List<ReviewedLine> reviewedItems;
}
