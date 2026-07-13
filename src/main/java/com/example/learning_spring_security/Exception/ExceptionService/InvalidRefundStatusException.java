package com.example.learning_spring_security.Exception.ExceptionService;

public class InvalidRefundStatusException extends BaseException {

    public InvalidRefundStatusException(String refundId, String currentStatus) {
        super(String.format("Refund '%s' cannot be processed because its status is '%s'", refundId, currentStatus),
                "INVALID_REFUND_STATUS", refundId, currentStatus);
    }
}