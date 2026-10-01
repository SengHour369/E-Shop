package com.example.eshop.order.exception;

import com.example.eshop.common.exception.ResourceNotFoundException;

public class CancelationNotFoundException extends ResourceNotFoundException {

    public CancelationNotFoundException(String orderNo) {
        super("Cancelation record not found for order: " + orderNo);
    }
}
