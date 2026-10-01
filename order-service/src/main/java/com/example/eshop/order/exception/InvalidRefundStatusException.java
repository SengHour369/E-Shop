package com.example.eshop.order.exception;

import com.example.eshop.common.exception.BusinessLogicException;

public class InvalidRefundStatusException extends BusinessLogicException {

    public InvalidRefundStatusException(String refundId, String currentStatus) {
        super(String.format("Refund '%s' cannot be processed because its status is '%s'", refundId, currentStatus));
    }
}
