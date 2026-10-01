package com.example.eshop.auth.exception;

public class BusinessLogicException extends BaseException {

    public BusinessLogicException(String message) {
        super(message, "BUSINESS_LOGIC_ERROR");
    }

    public BusinessLogicException(String message, Object... args) {
        super(message, "BUSINESS_LOGIC_ERROR", args);
    }
}