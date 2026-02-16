package com.example.learning_spring_security.Exception.ExceptionService;

public class DuplicateResourceException extends BaseException {

    public DuplicateResourceException(String message) {
        super(message, "DUPLICATE_RESOURCE");
    }

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue),
                "DUPLICATE_RESOURCE", resourceName, fieldName, fieldValue);
    }
}