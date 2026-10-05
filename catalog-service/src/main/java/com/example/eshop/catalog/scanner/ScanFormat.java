package com.example.eshop.catalog.scanner;

import com.example.eshop.common.exception.BusinessLogicException;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

public enum ScanFormat {
    EAN_13, EAN_8, UPC_A, UPC_E, CODE_128, CODE_39, QR_CODE, SKU;

    @JsonCreator
    public static ScanFormat from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String key = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        try {
            return ScanFormat.valueOf(key);
        } catch (IllegalArgumentException ex) {
            throw new BusinessLogicException("Unsupported scan format");
        }
    }

    public boolean isRetailBarcode() {
        return this == EAN_13 || this == EAN_8 || this == UPC_A || this == UPC_E;
    }
}
