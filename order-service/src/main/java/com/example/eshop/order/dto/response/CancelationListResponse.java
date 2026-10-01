package com.example.eshop.order.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class CancelationListResponse {
    private String orderNo;
    private String customerName;
    private LocalDateTime cancelDate;
    private String cancelReason;
    private BigDecimal amount;

    public CancelationListResponse(String orderNo, String customerName, LocalDateTime cancelDate,
                                    String cancelReason, BigDecimal amount) {
        this.orderNo = orderNo;
        this.customerName = customerName;
        this.cancelDate = cancelDate;
        this.cancelReason = cancelReason;
        this.amount = amount;
    }
}
