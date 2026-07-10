package com.example.learning_spring_security.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnDetailResponse {

    @JsonProperty("return_id")
    private String returnId;

    @JsonProperty("order_no")
    private String orderNo;

    @JsonProperty("customer_id")
    private Long customerId;

    @JsonProperty("customer_name")
    private String customerName;

    @JsonProperty("customer_email")
    private String customerEmail;

    @JsonProperty("product_id")
    private Long productId;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("return_type")
    private String returnType;

    private String reason;

    private String status;

    private BigDecimal amount;

    @JsonProperty("requested_at")
    private LocalDateTime requestedAt;

    @JsonProperty("requested_by")
    private String requestedBy;

    @JsonProperty("approved_at")
    private LocalDateTime approvedAt;

    @JsonProperty("approved_by")
    private String approvedBy;

    @JsonProperty("rejected_at")
    private LocalDateTime rejectedAt;

    @JsonProperty("rejected_by")
    private String rejectedBy;

    @JsonProperty("completed_at")
    private LocalDateTime completedAt;

    private String remark;
}