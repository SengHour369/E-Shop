package com.example.learning_spring_security.Exception.ExceptionService;

public class CancelationNotFoundException extends BaseException {

    public CancelationNotFoundException(String orderNo) {
        super("Cancelation record not found for order: " + orderNo, "CANCELATION_NOT_FOUND", orderNo);
    }
}