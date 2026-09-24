package com.example.learning_spring_security.Constant;

public interface CancelReason {
    String CUSTOMER_REQUESTED = "CUSTOMER_REQUESTED";
    String FRAUD_SUSPECTED = "FRAUD_SUSPECTED";
    String PAYMENT_FAILED = "PAYMENT_FAILED";
    String OUT_OF_STOCK = "OUT_OF_STOCK";
    String DUPLICATE_ORDER = "DUPLICATE_ORDER";
    String ADDRESS_ISSUE = "ADDRESS_ISSUE";
    String OTHER = "OTHER";
}