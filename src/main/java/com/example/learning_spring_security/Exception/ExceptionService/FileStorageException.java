package com.example.learning_spring_security.Exception.ExceptionService;

public class FileStorageException extends BaseException {

    public FileStorageException(String message) {
        super(message, "FILE_STORAGE_ERROR");
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause, "FILE_STORAGE_ERROR");
    }
}