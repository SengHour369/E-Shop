package com.example.learning_spring_security.Exception.ExceptionService;

public class InvalidReturnStatusException extends BaseException {

    public InvalidReturnStatusException(String returnId, String currentStatus) {
        super(String.format("Return request '%s' cannot be processed because its status is '%s'", returnId, currentStatus),
                "INVALID_RETURN_STATUS", returnId, currentStatus);
    }
}