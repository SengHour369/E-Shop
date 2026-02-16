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
                .comparePrice(request.getComparePrice())
                .quantity(request.getQuantity())
                .color(request.getColor())
                .size(request.getSize())
                .material(request.getMaterial())
                .isDefault(request.getIsDefault())
                .product(product)
                .build();
    }

    public static ProductSkuResponse toResponse(ProductSku sku) {
        return ProductSkuResponse.builder()
                .id(sku.getId())
                .sku(sku.getSku())
                .description(sku.getDescription())
                .price(sku.getPrice())
                .comparePrice(sku.getComparePrice())
                .quantity(sku.getQuantity())
                .lowStockThreshold(sku.getLowStockThreshold())
                .color(sku.getColor())
                .size(sku.getSize())
                .material(sku.getMaterial())
                .isDefault(sku.getIsDefault())
                .productId(sku.getProduct() != null ? sku.getProduct().getId() : null)
                .build();
    }

    public static void updateEntity(ProductSku sku, ProductSkuRequest request) {
        sku.setSku(request.getSku());
        sku.setDescription(request.getDescription());
        sku.setPrice(request.getPrice());
        sku.setComparePrice(request.getComparePrice());
        sku.setQuantity(request.getQuantity());
        sku.setColor(request.getColor());
        sku.setSize(request.getSize());
        sku.setMaterial(request.getMaterial());
        sku.setIsDefault(request.getIsDefault());
    }
}