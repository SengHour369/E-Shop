package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Model.Product;
import com.example.learning_spring_security.Model.ProductAttribute;
import com.example.learning_spring_security.Model.ProductSku;
import com.example.learning_spring_security.dto.Request.ProductSkuRequest;
import com.example.learning_spring_security.dto.Response.ProductSkuResponse;

import java.util.List;

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

    public static ProductSkuResponse toResponse(ProductSku sku) {
        return ProductSkuResponse.builder()
                .id(sku.getId())
                .sku(sku.getSku())
                .description(sku.getDescription())
                .price(sku.getPrice())
                .isDefault(sku.getIsDefault())
                .OperatorProductAttribute(sku.getOperatorProductAttribute())
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