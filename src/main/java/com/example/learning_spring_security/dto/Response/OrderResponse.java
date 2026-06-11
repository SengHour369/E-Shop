package com.example.learning_spring_security.dto.Response;

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

    @JsonProperty("customer_id")
    private Long customerId;
    @JsonProperty("customer_name")
    private String customerName;
    @JsonProperty("customer_email")
    private String customerEmail;

    @JsonProperty("shipping_address")
    private AddressResponse shippingAddress;

    private List<OrderItemResponse> items;
    private PaymentResponse payment;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("qr_code")
    private String qrCode;
    @JsonProperty("payment_url")
    private String paymentUrl;
}