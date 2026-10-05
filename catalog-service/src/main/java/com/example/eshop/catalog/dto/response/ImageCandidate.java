package com.example.eshop.catalog.dto.response;

import lombok.Getter;

@Getter
public class ImageCandidate {
    private final Long productId;
    private final Long productSkuId;
    private final String name;
    private final String sku;
    private final String imageUrl;
    private final double confidence;

    public ImageCandidate(Long productId, Long productSkuId, String name, String sku, String imageUrl, double confidence) {
        this.productId = productId;
        this.productSkuId = productSkuId;
        this.name = name;
        this.sku = sku;
        this.imageUrl = imageUrl;
        this.confidence = confidence;
    }
}
