package com.example.learning_spring_security.Exception.ExceptionService;

import java.math.BigDecimal;

public class RefundAmountExceededException extends BaseException {

    public RefundAmountExceededException(BigDecimal amount, BigDecimal remainingRefundable) {
        super(String.format("Refund amount %s exceeds the remaining refundable amount %s", amount, remainingRefundable),
                "REFUND_AMOUNT_EXCEEDED", amount, remainingRefundable);
    }
}