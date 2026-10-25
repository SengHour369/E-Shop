package com.example.eshop.catalog.dto.response;

import com.example.eshop.catalog.model.StockMovement;
import java.time.LocalDateTime;

/** Flat API view: never serialize the inventory/SKU/product entity graph. */
public record StockMovementResponse(
        Long id, Long inventoryId, Long quantityChange, Long previousQuantity,
        Long newQuantity, String movementType, String remark, String performedBy,
        String warehouseLocation, LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static StockMovementResponse from(StockMovement movement) {
        return new StockMovementResponse(
                movement.getId(), movement.getInventory().getId(),
                movement.getQuantityChange(), movement.getPreviousQuantity(),
                movement.getNewQuantity(), movement.getMovementType(),
                movement.getRemark(), movement.getPerformedBy(), movement.getWarehouseLocation(),
                movement.getCreatedAt(), movement.getUpdatedAt());
    }
}
