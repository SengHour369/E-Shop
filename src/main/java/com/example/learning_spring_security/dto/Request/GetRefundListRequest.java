package com.example.learning_spring_security.dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetRefundListRequest {

    @Builder.Default
    private Integer page = 1;

    @Builder.Default
    private Integer size = 10;

    @JsonProperty("refund_id")
    private String refundId;

    @JsonProperty("order_no")
    private String orderNo;

    @JsonProperty("customer_name")
    private String customerName;

    private String status;

    @JsonProperty("from_date")
    private LocalDateTime fromDate;

    @JsonProperty("to_date")
    private LocalDateTime toDate;
}