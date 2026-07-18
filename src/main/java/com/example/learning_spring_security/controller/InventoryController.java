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
@Tag(name = "Inventory Management", description = "Inventory and stock operations")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    @Operation(summary = "Create inventory for a SKU")
    public ResponseEntity<ResponseErrorTemplate> createInventory(
            @Valid @RequestBody InventoryRequest request,
            @RequestParam Long skuId) {
        return new ResponseEntity<>(inventoryService.createInventory(skuId, request), HttpStatus.CREATED);
    }

    @PostMapping("/all/")
    @Operation(summary = "Get all inventory with pagination")
    public ResponseEntity<Page<ResponseErrorTemplate>> getAllInventory(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(inventoryService.getAllInventory(pageable));
    }

    @PostMapping("/id/")
    @Operation(summary = "Get inventory by ID")
    public ResponseEntity<ResponseErrorTemplate> getInventoryById(@RequestParam Long id) {
        return ResponseEntity.ok(inventoryService.getInventoryById(id));
    }

    @PostMapping("/sku/")
    @Operation(summary = "Get inventory by SKU ID")
    public ResponseEntity<ResponseErrorTemplate> getInventoryBySkuId(@RequestParam Long skuId) {
        return ResponseEntity.ok(inventoryService.getInventoryBySkuId(skuId));
    }

    @PostMapping("/product/")
    @Operation(summary = "Get all inventories for a product")
    public ResponseEntity<Page<ResponseErrorTemplate>> getInventoryByProductId(
            @RequestParam Long productId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(inventoryService.getInventoryByProductId(productId, pageable));
    }

    @PostMapping("/low-stock")
    @Operation(summary = "Get low stock items")
    public ResponseEntity<Page<ResponseErrorTemplate>> getLowStock(
            @RequestParam(defaultValue = "10") Long threshold,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(inventoryService.getLowStockInventory(threshold, pageable));
    }

    @PatchMapping("/restock")
    @Operation(summary = "Restock inventory")
    public ResponseEntity<ResponseErrorTemplate> restock(
            @RequestParam Long id,
            @Valid @RequestBody RestockRequest request) {
        return ResponseEntity.ok(inventoryService.restock(id, request));
    }

    @PostMapping("/exact")
    @Operation(summary = "Adjust inventory (set exact quantity)")
    public ResponseEntity<ResponseErrorTemplate> adjustInventory(
            @RequestParam Long id,
            @Valid @RequestBody InventoryRequest request) {
        return ResponseEntity.ok(inventoryService.adjustQuantity(id, request));
    }

    @PostMapping("/delete")
    @Operation(summary = "Delete inventory record")
    public ResponseEntity<Void> deleteInventory(@RequestParam Long id) {
        inventoryService.deleteInventory(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- NEW ENDPOINTS ----------
    @GetMapping("/summary")
    @Operation(summary = "Get inventory dashboard summary")
    public ResponseEntity<ResponseErrorTemplate> getInventorySummary() {
        return ResponseEntity.ok(inventoryService.getInventorySummary());
    }

    @PostMapping("/search")
    @Operation(summary = "Search inventory with filters")
    public ResponseEntity<Page<ResponseErrorTemplate>> searchInventory(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String warehouse,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(inventoryService.searchInventory(search, warehouse, status, pageable));
    }

    @GetMapping("/history/{inventoryId}")
    @Operation(summary = "Get movement history for a specific inventory")
    public ResponseEntity<ResponseErrorTemplate> getInventoryHistory(
            @PathVariable Long inventoryId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(inventoryService.getInventoryHistory(inventoryId, pageable));
    }
    @PostMapping("/increase-stock")
    @Operation(summary = "Increase stock quantity for a SKU")
    public ResponseEntity<ResponseErrorTemplate> increaseStock(
            @Parameter(description = "Product SKU ID") @RequestParam Long skuId,
            @Parameter(description = "Quantity to add") @RequestParam Long quantity) {
        inventoryService.increaseStock(skuId, quantity);
        return ResponseEntity.ok(ResponseErrorTemplate.success("Stock increased successfully", null));
    }
}