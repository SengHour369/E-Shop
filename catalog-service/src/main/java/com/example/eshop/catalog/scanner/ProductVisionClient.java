package com.example.eshop.catalog.scanner;

public interface ProductVisionClient {
    /** Asks the vision process for names. Catalog decides which products exist. */
    VisionOutcome identify(byte[] image, String mediaType);
}
