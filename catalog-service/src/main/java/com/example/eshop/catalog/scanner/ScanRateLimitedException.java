package com.example.eshop.catalog.scanner;

public class ScanRateLimitedException extends RuntimeException {
    public ScanRateLimitedException() {
        super("Too many scan requests");
    }
}
