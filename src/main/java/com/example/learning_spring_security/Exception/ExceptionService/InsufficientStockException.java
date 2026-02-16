package com.example.learning_spring_security.Exception.ExceptionService;

public class InsufficientStockException extends BaseException {

    public InsufficientStockException(String message) {
        super(message, "INSUFFICIENT_STOCK");
    }

    public InsufficientStockException(String productSku, Long requested, Long available) {
        super(String.format("Insufficient stock for SKU: %s. Requested: %d, Available: %d",
                productSku, requested, available), "INSUFFICIENT_STOCK", productSku, requested, available);
    }
}