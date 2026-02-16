package com.example.learning_spring_security.Exception.ExceptionService;

public class BusinessLogicException extends BaseException {

    public BusinessLogicException(String message) {
        super(message, "BUSINESS_LOGIC_ERROR");
    }

    public BusinessLogicException(String message, Object... args) {
        super(message, "BUSINESS_LOGIC_ERROR", args);
    }
}