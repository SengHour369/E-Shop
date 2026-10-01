package com.example.eshop.payment.exception;

/**
 * Raised when no (successful) payment transaction can be found for a given order.
 * Kept as a domain-specific exception rather than folded into common-lib's
 * ResourceNotFoundException so callers can distinguish "no payment transaction yet"
 * from a generic missing-resource case.
 */
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(Long orderId) {
        super("No successful payment transaction found for orderId: " + orderId);
    }
}
