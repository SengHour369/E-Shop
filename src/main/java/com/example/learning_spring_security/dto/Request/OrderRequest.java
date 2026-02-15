package com.example.learning_spring_security.dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

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

    @JsonProperty("cart_item_ids")
    private List<Long> cartItemIds;  // សម្រាប់បង្កើត Order ពី Cart

    // ឬប្រើនេះសម្រាប់បង្កើត Order ផ្ទាល់
    private List<OrderItemRequest> items;
}