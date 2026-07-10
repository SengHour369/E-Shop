package com.example.learning_spring_security.dto.Response;

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

    @JsonProperty("customer_name")
    private String customerName;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("return_type")
    private String returnType;

    private String reason;

    private String status;

    private BigDecimal amount;
}