package com.example.eshop.order.exception;

import com.example.eshop.common.exception.ResourceNotFoundException;

public class RefundNotFoundException extends ResourceNotFoundException {

    public RefundNotFoundException(String refundId) {
        super("Refund not found with refundId: " + refundId);
    }
}
