package com.example.eshop.auth.exception;

public class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        super(message, "FORBIDDEN");
    }

    public ForbiddenException(String message, Object... args) {
        super(message, "FORBIDDEN", args);
    }
}