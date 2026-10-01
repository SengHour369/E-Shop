package com.example.eshop.auth.exception;

public class BadRequestException extends BaseException {

    public BadRequestException(String message) {
        super(message, "BAD_REQUEST");
    }

    public BadRequestException(String message, Object... args) {
        super(message, "BAD_REQUEST", args);
    }
}