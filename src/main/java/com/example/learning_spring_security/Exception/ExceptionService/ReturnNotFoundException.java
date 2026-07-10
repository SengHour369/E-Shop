package com.example.learning_spring_security.Exception.ExceptionService;

public class ReturnNotFoundException extends BaseException {

    public ReturnNotFoundException(String returnId) {
        super("Return request not found with returnId: " + returnId, "RETURN_NOT_FOUND", returnId);
    }
}