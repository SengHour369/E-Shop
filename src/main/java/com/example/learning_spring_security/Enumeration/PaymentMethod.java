package com.example.learning_spring_security.Enumeration;


public enum PaymentMethod {
    RAZORPAY,
    SINGE,
    // KHQR rails (Bakong national standard). ABA/ACLEDA apps scan the same Bakong QR.
    BAKONG,
    ABA,
    ACLEDA;

    /**
     * Lenient lookup used when mapping a stored payment-method string (e.g. from
     * {@code Payment.paymentMethod}) onto this enum. Returns {@code null} for unknown
     * or blank values instead of throwing.
     */
    public static PaymentMethod fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return PaymentMethod.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}