package com.example.eshop.order.exception;

import com.example.eshop.common.exception.ResourceNotFoundException;

public class ReturnNotFoundException extends ResourceNotFoundException {

    public ReturnNotFoundException(String returnId) {
        super("Return request not found with returnId: " + returnId);
    }
}
