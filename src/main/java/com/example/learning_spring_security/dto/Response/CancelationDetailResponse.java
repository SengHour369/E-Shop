package com.example.learning_spring_security.dto.Response;

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
public class CancelationDetailResponse {

    private String cancelationId;

    private Long orderId;

    private String orderNo;

    private Long customerId;

    private String customerName;

    private String cancelReason;

    private String cancelStatus;

    private String cancelSource;

    private LocalDateTime cancelDate;

    private BigDecimal amount;

    private String currency;

    private String remark;

    private LocalDateTime reviewedAt;

    private String reviewedBy;

    private LocalDateTime createdAt;

    private String createdBy;

    private LocalDateTime updatedAt;

    private String updatedBy;
}