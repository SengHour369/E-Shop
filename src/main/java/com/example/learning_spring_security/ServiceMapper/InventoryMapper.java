package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Model.Inventory;
import com.example.learning_spring_security.Model.ProductSku;
import com.example.learning_spring_security.dto.Request.InventoryRequest;
import com.example.learning_spring_security.dto.Response.InventoryResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;

public class InventoryMapper {

    public static Inventory toEntity(InventoryRequest request, ProductSku productSku) {
        return Inventory.builder()
                .productSku(productSku)
                .quantity(request.getQuantity())
                .reservedQuantity(0L)
                .warehouseLocation(request.getWarehouseLocation())
                .build();
    }

    public static InventoryResponse toResponse(Inventory inventory) {
        ProductSku sku = inventory.getProductSku();
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productSkuId(sku.getId())
                .sku(sku.getSku())
                .productSkuId(sku.getId())
                .quantity(inventory.getQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .availableQuantity(inventory.getQuantity() - inventory.getReservedQuantity())
                .warehouseLocation(inventory.getWarehouseLocation())
                .lastRestockedAt(inventory.getLastRestockedAt())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    public static ResponseErrorTemplate toWrappedResponse(Inventory inventory) {
        return new ResponseErrorTemplate(Constant.SUC_MSG, Constant.SUC_CODE, toResponse(inventory));
    }
}