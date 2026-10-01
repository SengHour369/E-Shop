package com.example.eshop.order.exception;

import com.example.eshop.common.exception.BusinessLogicException;

import java.math.BigDecimal;

public class RefundAmountExceededException extends BusinessLogicException {

    public RefundAmountExceededException(BigDecimal amount, BigDecimal remainingRefundable) {
        super(String.format("Refund amount %s exceeds the remaining refundable amount %s", amount, remainingRefundable));
    }
}
