package com.example.learning_spring_security.Exception.ExceptionService;

public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED");
    }

    public UnauthorizedException(String message, Object... args) {
        super(message, "UNAUTHORIZED", args);
    }
}