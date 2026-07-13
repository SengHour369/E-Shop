package com.example.learning_spring_security.Exception.ExceptionService;

public class PaymentNotFoundException extends BaseException {

    public PaymentNotFoundException(Long orderId) {
        super("No successful payment transaction found for orderId: " + orderId, "PAYMENT_NOT_FOUND", orderId);
    }
}