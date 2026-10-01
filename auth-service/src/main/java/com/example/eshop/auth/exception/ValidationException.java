package com.example.eshop.auth.exception;

import java.util.Map;

public class ValidationException extends BaseException {
    private final Map<String, String> errors;

    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR");
        this.errors = null;
    }

    public ValidationException(String message, Map<String, String> errors) {
        super(message, "VALIDATION_ERROR");
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}