package com.example.eshop.order.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnListResponse {

    @JsonProperty("return_id")
    private String returnId;

    @JsonProperty("order_no")
    private String orderNo;

    // Was `customerName` (User join) / `productName` (Product join) — cross-service data.
    // TODO: cross-service calls via Feign to auth-service/catalog-service to enrich these.
    @JsonProperty("return_type")
    private String returnType;

    private String reason;
    private String status;
    private BigDecimal amount;
}
