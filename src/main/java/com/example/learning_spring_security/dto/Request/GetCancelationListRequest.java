package com.example.learning_spring_security.dto.Request;

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
public class GetCancelationListRequest {

    @Builder.Default
    private Integer page = 1;

    @Builder.Default
    private Integer size = 10;

    private String orderNo;

    private String customerName;

    private String cancelReason;

    private String cancelStatus;

    private LocalDateTime fromCancelDate;

    private LocalDateTime toCancelDate;

    private BigDecimal minAmount;

    private BigDecimal maxAmount;
}