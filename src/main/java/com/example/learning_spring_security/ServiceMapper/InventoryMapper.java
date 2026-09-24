package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Model.Inventory;
import com.example.learning_spring_security.Model.ProductSku;
import com.example.learning_spring_security.dto.Request.InventoryRequest;
import com.example.learning_spring_security.dto.Response.InventoryResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;

public class InventoryMapper {

    public static Inventory toEntity(InventoryRequest request, ProductSku productSku) {
        Inventory.InventoryBuilder builder = Inventory.builder()
                .productSku(productSku)
                .quantity(request.getQuantity())
                .reservedQuantity(0L)
                .warehouseLocation(request.getWarehouseLocation());

        if (request.getLowStockThreshold() != null) {
            builder.lowStockThreshold(request.getLowStockThreshold());
        }

        return builder.build();
    }

    public static InventoryResponse toResponse(Inventory inventory) {
        ProductSku sku = inventory.getProductSku();
        if (sku == null) {
            throw new IllegalStateException("Inventory has no associated ProductSku");
        }

        long available = inventory.getQuantity() - inventory.getReservedQuantity();
        int threshold = inventory.getLowStockThreshold() != null ? inventory.getLowStockThreshold() : 5;
        String stockStatus;
        if (available <= 0) {
            stockStatus = "OUT_OF_STOCK";
        } else if (available <= threshold) {
            stockStatus = "LOW_STOCK";
        } else {
            stockStatus = "IN_STOCK";
        }

        return InventoryResponse.builder()
                .id(inventory.getId())
                .productSkuId(sku.getId())
                .sku(sku.getSku())
                .productId(sku.getProduct() != null ? sku.getProduct().getId() : null)
                .productName(sku.getProduct() != null ? sku.getProduct().getName() : null)
                .description(sku.getDescription())
                .quantity(inventory.getQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .availableQuantity(available)
                .warehouseLocation(inventory.getWarehouseLocation())
                .lowStockThreshold(inventory.getLowStockThreshold())
                .stockStatus(stockStatus)
                .lastRestockedAt(inventory.getLastRestockedAt())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    public static ResponseErrorTemplate toWrappedResponse(Inventory inventory) {
        return new ResponseErrorTemplate(Constant.SUC_MSG, Constant.SUC_CODE, toResponse(inventory));
    }
}