package com.example.eshop.common.exception;

public class CustomMessageException extends RuntimeException {

    private final String code;

    public CustomMessageException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
