package com.example.learning_spring_security.Exception.ExceptionService;

public class RefundNotFoundException extends BaseException {

    public RefundNotFoundException(String refundId) {
        super("Refund not found with refundId: " + refundId, "REFUND_NOT_FOUND", refundId);
    }
}