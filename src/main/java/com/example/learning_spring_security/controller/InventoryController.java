package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.Service.ServiceStructure.InventoryService;
import com.example.learning_spring_security.dto.Request.InventoryRequest;
import com.example.learning_spring_security.dto.Request.RestockRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "inventory-controller", description = "Inventory management APIs")
public class InventoryController extends BaseController {

    private final InventoryService inventoryService;

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Create inventory", description = "Create inventory record for a product SKU (Admin only)")
    public ResponseEntity<ResponseErrorTemplate> createInventory(@Valid @RequestBody InventoryRequest request) {
        return new ResponseEntity<>(inventoryService.createInventory(request), HttpStatus.CREATED);
    }

    @PostMapping("/all/")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Get all inventory", description = "Get all inventory records with pagination (Admin only)")
    public ResponseEntity<Page<ResponseErrorTemplate>> getAllInventory(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(inventoryService.getAllInventory(pageable));
    }

    @PostMapping("/id/")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Get inventory by ID")
    public ResponseEntity<ResponseErrorTemplate> getInventoryById(
            @Parameter(description = "Inventory ID") @RequestParam Long id) {
        return ResponseEntity.ok(inventoryService.getInventoryById(id));
    }

    @PostMapping("/sku/")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Get inventory by SKU ID")
    public ResponseEntity<ResponseErrorTemplate> getInventoryBySkuId(
            @Parameter(description = "ProductSku ID") @RequestParam Long skuId) {
        return ResponseEntity.ok(inventoryService.getInventoryBySkuId(skuId));
    }

    @PostMapping("/product/")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Get inventory by product ID", description = "Get all SKU inventories for a product")
    public ResponseEntity<Page<ResponseErrorTemplate>> getInventoryByProductId(
            @Parameter(description = "Product ID") @RequestParam Long productId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(inventoryService.getInventoryByProductId(productId, pageable));
    }

    @PostMapping("/low-stock")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Get low stock items", description = "Get inventory items where available quantity <= threshold")
    public ResponseEntity<Page<ResponseErrorTemplate>> getLowStock(
            @Parameter(description = "Stock threshold", example = "10")
            @RequestParam(defaultValue = "10") Long threshold,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(inventoryService.getLowStockInventory(threshold, pageable));
    }

    @PatchMapping("/restock")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Restock inventory", description = "Add quantity to existing inventory")
    public ResponseEntity<ResponseErrorTemplate> restock(
            @Parameter(description = "Inventory ID") @RequestParam Long id,
            @Valid @RequestBody RestockRequest request) {
        return ResponseEntity.ok(inventoryService.restock(id, request));
    }

    @PostMapping("/exact")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Adjust inventory", description = "Set exact quantity and/or warehouse location")
    public ResponseEntity<ResponseErrorTemplate> adjustInventory(
            @Parameter(description = "Inventory ID") @RequestParam Long id,
            @Valid @RequestBody InventoryRequest request) {
        return ResponseEntity.ok(inventoryService.adjustQuantity(id, request));
    }

    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Delete inventory record")
    public ResponseEntity<Void> deleteInventory(
            @Parameter(description = "Inventory ID") @RequestParam Long id) {
        inventoryService.deleteInventory(id);
        return ResponseEntity.noContent().build();
    }
}