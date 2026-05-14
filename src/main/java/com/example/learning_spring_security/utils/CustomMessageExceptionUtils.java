package com.example.learning_spring_security.utils;


import com.example.learning_spring_security.Exception.CustomMessageException;
import org.springframework.http.HttpStatus;

public class CustomMessageExceptionUtils {

    private CustomMessageExceptionUtils() {}

    public static CustomMessageException unauthorized() {
        return new CustomMessageException("Unauthorized", String.valueOf(HttpStatus.UNAUTHORIZED.value()));
    }

    public static CustomMessageException forbidden() {
        return new CustomMessageException("Forbidden", String.valueOf(HttpStatus.FORBIDDEN.value()));
    }

    public static CustomMessageException notFound(String resource) {
        return new CustomMessageException(resource + " not found", String.valueOf(HttpStatus.NOT_FOUND.value()));
    }

    public static CustomMessageException badRequest(String message) {
        return new CustomMessageException(message, String.valueOf(HttpStatus.BAD_REQUEST.value()));
    }

    public static CustomMessageException conflict(String message) {
        return new CustomMessageException(message, String.valueOf(HttpStatus.CONFLICT.value()));
    }

    public static CustomMessageException internalServerError(String message) {
        return new CustomMessageException(message, String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

}
