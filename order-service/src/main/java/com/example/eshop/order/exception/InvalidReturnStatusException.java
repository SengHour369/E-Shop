package com.example.eshop.order.exception;

import com.example.eshop.common.exception.BusinessLogicException;

public class InvalidReturnStatusException extends BusinessLogicException {

    public InvalidReturnStatusException(String returnId, String currentStatus) {
        super(String.format("Return request '%s' cannot be processed because its status is '%s'", returnId, currentStatus));
    }
}
