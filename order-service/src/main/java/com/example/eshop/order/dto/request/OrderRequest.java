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
}
