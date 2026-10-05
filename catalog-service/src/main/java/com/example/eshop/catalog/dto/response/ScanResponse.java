package com.example.eshop.catalog.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScanResponse {
    private final boolean found;
    private final String code;
    private final String format;
    private final String matchType;
    private final String message;
    private final String requestId;
    private final Long productId;
    private final Long productSkuId;
    private final String sku;
    private final String barcode;
    private final String name;
    private final BigDecimal originalPrice;
    private final BigDecimal finalPrice;
    private final Boolean hasPromotion;
    private final BigDecimal discountPercentage;
    private final Long quantity;
    private final Long reservedQuantity;
    private final Long availableQuantity;
    private final Boolean inStock;
    private final Boolean lowStock;
    private final Boolean active;
    private final Boolean availableForSale;
    private final String imageUrl;
    private final String warehouseLocation;
    private final List<ScanVariant> variant;
}
