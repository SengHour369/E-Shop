package com.example.learning_spring_security.dto;

import java.time.LocalDateTime;

public class APIResponse<T> {

    private boolean success;
    private String message;
    private int status;
    private LocalDateTime timestamp;
    private T data;

    public APIResponse() {
    }

    public APIResponse(boolean success, String message, int status, T data) {
        this.success = success;
        this.message = message;
        this.status = status;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> APIResponse<T> success(String message, int status, T data) {
        return new APIResponse<>(true, message, status, data);
    }

    public static <T> APIResponse<T> error(String message, int status) {
        return new APIResponse<>(false, message, status, null);
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public int getStatus() { return status; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public T getData() { return data; }
}