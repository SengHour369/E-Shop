package com.example.eshop.catalog.mapper;

import com.example.eshop.catalog.model.Product;
import com.example.eshop.catalog.model.ProductSku;
import com.example.eshop.catalog.dto.request.ProductSkuRequest;

public class ProductSkuMapper {

    public static ProductSku toEntity(ProductSkuRequest request, Product product) {
        return ProductSku.builder()
                .description(request.getDescription())
                .price(request.getPrice())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .product(product)
                .OperatorProductAttribute(request.getOperatorProductAttribute())
                .build();
    }

    public static void updateEntity(ProductSku sku, ProductSkuRequest request) {
        // Only update SKU if provided (not null/blank)
        sku.setDescription(request.getDescription());
        sku.setPrice(request.getPrice());
        sku.setOperatorProductAttribute(request.getOperatorProductAttribute());
        if (request.getIsDefault() != null) sku.setIsDefault(request.getIsDefault());
    }
}