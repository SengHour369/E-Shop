package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Model.Product;
import com.example.learning_spring_security.Model.ProductSku;
import com.example.learning_spring_security.dto.Request.ProductSkuRequest;
import com.example.learning_spring_security.dto.Response.ProductSkuResponse;

public class ProductSkuMapper {

    public static ProductSku toEntity(ProductSkuRequest request, Product product) {
        return ProductSku.builder()
                .sku(request.getSku())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .lowStockThreshold(request.getLowStockThreshold() != null ? request.getLowStockThreshold() : 5)
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .product(product)
                .build();
    }

    public static ProductSkuResponse toResponse(ProductSku sku) {
        return ProductSkuResponse.builder()
                .id(sku.getId())
                .sku(sku.getSku())
                .description(sku.getDescription())
                .price(sku.getPrice())
                .quantity(sku.getQuantity())
                .build();
    }

    public static void updateEntity(ProductSku sku, ProductSkuRequest request) {
        sku.setSku(request.getSku());
        sku.setDescription(request.getDescription());
        sku.setPrice(request.getPrice());
        sku.setQuantity(request.getQuantity());
//        sku.setColor(request.getColor());
//        sku.setSize(request.getSize());
        if (request.getLowStockThreshold() != null) sku.setLowStockThreshold(request.getLowStockThreshold());
        if (request.getIsDefault() != null) sku.setIsDefault(request.getIsDefault());
    }
}