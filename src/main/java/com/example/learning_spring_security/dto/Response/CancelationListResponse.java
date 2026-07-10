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
public class CancelationListResponse {

    private String orderNo;

    private String customerName;

    private LocalDateTime cancelDate;

    private String cancelReason;

    private BigDecimal amount;
}