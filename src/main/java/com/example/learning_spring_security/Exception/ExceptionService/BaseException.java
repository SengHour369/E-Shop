package com.example.learning_spring_security.Exception.ExceptionService;

public abstract class BaseException extends RuntimeException {
    private final String errorCode;
    private final Object[] args;

    public BaseException(String message, String errorCode, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

    public BaseException(String message, Throwable cause, String errorCode, Object... args) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = args;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args;
    }
}