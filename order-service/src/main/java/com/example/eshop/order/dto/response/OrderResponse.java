package com.example.eshop.order.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class OrderResponse {
    private Long id;
    @JsonProperty("order_number")
    private String orderNumber;
    @JsonProperty("order_date")
    private LocalDateTime orderDate;
    private String status;
    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    // Was `customerName`/`customerEmail` resolved via a User join — auth-service owns User.
    // TODO: cross-service call via Feign to auth-service to enrich customer name/email.
    @JsonProperty("customer_id")
    private Long customerId;

    // Was a nested AddressResponse — auth-service owns Address.
    // TODO: cross-service call via Feign to auth-service to resolve shipping address details.
    @JsonProperty("shipping_address_id")
    private Long shippingAddressId;

    private List<OrderItemResponse> items;

    // Was a nested PaymentResponse — payment-service owns Payment.
    // TODO: cross-service call via Feign to payment-service to enrich payment details.
    @JsonProperty("payment_id")
    private Long paymentId;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
