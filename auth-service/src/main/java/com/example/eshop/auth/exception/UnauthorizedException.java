package com.example.eshop.auth.exception;

public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED");
    }

    public UnauthorizedException(String message, Object... args) {
        super(message, "UNAUTHORIZED", args);
    }
}