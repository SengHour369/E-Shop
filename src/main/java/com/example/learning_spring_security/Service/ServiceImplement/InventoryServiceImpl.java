package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.DuplicateResourceException;
import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.Inventory;
import com.example.learning_spring_security.Model.ProductSku;
import com.example.learning_spring_security.Model.StockMovement;
import com.example.learning_spring_security.Repository.InventoryRepository;
import com.example.learning_spring_security.Repository.ProductSkuRepository;
import com.example.learning_spring_security.Repository.StockMovementRepository;
import com.example.learning_spring_security.Service.ServiceStructure.InventoryService;
import com.example.learning_spring_security.ServiceMapper.InventoryMapper;
import com.example.learning_spring_security.dto.Request.InventoryRequest;
import com.example.learning_spring_security.dto.Request.RestockRequest;
import com.example.learning_spring_security.dto.Response.InventorySummaryResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductSkuRepository productSkuRepository;
    private final StockMovementRepository stockMovementRepository;

    // Helper
    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    // ---------- EXISTING METHODS (with movement recording) ----------

    @Override
    @Transactional
    public ResponseErrorTemplate createInventory(Long productSkuId, InventoryRequest request) {
        if (inventoryRepository.existsByProductSkuId(productSkuId)) {
            throw new DuplicateResourceException("Inventory already exists for SKU id: " + productSkuId);
        }

        ProductSku productSku = productSkuRepository.findById(productSkuId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductSku not found with id: " + productSkuId));

        Inventory inventory = InventoryMapper.toEntity(request, productSku);
        inventory.setLastRestockedAt(LocalDateTime.now());
        inventory.setIsDefault(false);
        Inventory saved = inventoryRepository.save(inventory);

        // Record initial movement
        StockMovement movement = StockMovement.builder()
                .inventory(saved)
                .quantityChange(saved.getQuantity())
                .previousQuantity(0L)
                .newQuantity(saved.getQuantity())
                .movementType("INITIAL")
                .remark("Inventory created")
                .performedBy(currentUsername())
                .warehouseLocation(saved.getWarehouseLocation())
                .build();
        stockMovementRepository.save(movement);

        log.info("Inventory created for SKU id={}", productSkuId);
        return InventoryMapper.toWrappedResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getInventoryById(Long id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + id));
        return InventoryMapper.toWrappedResponse(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getInventoryBySkuId(Long skuId) {
        Inventory inventory = inventoryRepository.findByProductSkuId(skuId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for SKU id: " + skuId));
        return InventoryMapper.toWrappedResponse(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseErrorTemplate> getAllInventory(Pageable pageable) {
        return inventoryRepository.findAll(pageable)
                .map(InventoryMapper::toWrappedResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseErrorTemplate> getInventoryByProductId(Long productId, Pageable pageable) {
        return inventoryRepository.findByProductId(productId, pageable)
                .map(InventoryMapper::toWrappedResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseErrorTemplate> getLowStockInventory(Long threshold, Pageable pageable) {
        return inventoryRepository.findLowStock(threshold, pageable)
                .map(InventoryMapper::toWrappedResponse);
    }

    @Override
    @Transactional
    public ResponseErrorTemplate restock(Long id, RestockRequest request) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + id));

        Long oldQty = inventory.getQuantity();
        Long newQty = oldQty + request.getQuantity();
        inventory.setQuantity(newQty);
        inventory.setLastRestockedAt(LocalDateTime.now());
        Inventory updated = inventoryRepository.save(inventory);

        StockMovement movement = StockMovement.builder()
                .inventory(inventory)
                .quantityChange(request.getQuantity())
                .previousQuantity(oldQty)
                .newQuantity(newQty)
                .movementType("RESTOCK")
                .remark("Restocked by " + request.getQuantity() + " units")
                .performedBy(currentUsername())
                .warehouseLocation(inventory.getWarehouseLocation())
                .build();
        stockMovementRepository.save(movement);

        log.info("Inventory restocked: id={}, added={}, total={}", id, request.getQuantity(), updated.getQuantity());
        return InventoryMapper.toWrappedResponse(updated);
    }

    @Override
    @Transactional
    public ResponseErrorTemplate adjustQuantity(Long id, InventoryRequest request) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + id));

        Long oldQty = inventory.getQuantity();
        Long newQty = request.getQuantity();
        inventory.setQuantity(newQty);
        if (request.getWarehouseLocation() != null) {
            inventory.setWarehouseLocation(request.getWarehouseLocation());
        }
        if (request.getLowStockThreshold() != null) {
            inventory.setLowStockThreshold(request.getLowStockThreshold());
        }
        Inventory updated = inventoryRepository.save(inventory);

        StockMovement movement = StockMovement.builder()
                .inventory(inventory)
                .quantityChange(newQty - oldQty)
                .previousQuantity(oldQty)
                .newQuantity(newQty)
                .movementType("ADJUSTMENT")
                .remark("Adjusted quantity to " + newQty)
                .performedBy(currentUsername())
                .warehouseLocation(inventory.getWarehouseLocation())
                .build();
        stockMovementRepository.save(movement);

        log.info("Inventory adjusted: id={}, newQuantity={}", id, request.getQuantity());
        return InventoryMapper.toWrappedResponse(updated);
    }

    @Override
    @Transactional
    public void deleteInventory(Long id) {
        if (!inventoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Inventory not found with id: " + id);
        }
        inventoryRepository.deleteById(id);
        log.info("Inventory deleted: id={}", id);
    }

    @Override
    @Transactional
    public void reduceStock(Long productSkuId, Long quantity) {
        int updated = inventoryRepository.reduceStock(productSkuId, quantity);
        if (updated == 0) {
            Inventory inventory = inventoryRepository.findByProductSkuId(productSkuId)
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for SKU id: " + productSkuId));
            if (inventory.getQuantity() < quantity) {
                throw new ResourceNotFoundException("Insufficient stock. Available: " + inventory.getQuantity() +
                        ", requested: " + quantity);
            }
            throw new RuntimeException("Unexpected error while reducing stock");
        }

        // Record movement
        Inventory inventory = inventoryRepository.findByProductSkuId(productSkuId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found after update"));
        StockMovement movement = StockMovement.builder()
                .inventory(inventory)
                .quantityChange(-quantity)
                .previousQuantity(inventory.getQuantity() + quantity)
                .newQuantity(inventory.getQuantity())
                .movementType("ORDER_PLACED")
                .remark("Stock reduced by " + quantity + " units")
                .performedBy(currentUsername())
                .warehouseLocation(inventory.getWarehouseLocation())
                .build();
        stockMovementRepository.save(movement);

        log.info("Reduced stock for SKU id {} by {}", productSkuId, quantity);
    }

    @Override
    @Transactional
    public void increaseStock(Long productSkuId, Long quantity) {
        int updated = inventoryRepository.increaseStock(productSkuId, quantity);
        if (updated == 0) {
            throw new ResourceNotFoundException("Inventory not found for SKU id: " + productSkuId);
        }

        Inventory inventory = inventoryRepository.findByProductSkuId(productSkuId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found after update"));
        StockMovement movement = StockMovement.builder()
                .inventory(inventory)
                .quantityChange(quantity)
                .previousQuantity(inventory.getQuantity() - quantity)
                .newQuantity(inventory.getQuantity())
                .movementType("RETURN")
                .remark("Stock increased by " + quantity + " units")
                .performedBy(currentUsername())
                .warehouseLocation(inventory.getWarehouseLocation())
                .build();
        stockMovementRepository.save(movement);

        log.info("Increased stock for SKU id {} by {}", productSkuId, quantity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSku> getLowStockSkus() {
        return inventoryRepository.findLowStockSkus();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSku> getLowStockSkusByProductId(Long productId) {
        return inventoryRepository.findLowStockSkusByProductId(productId);
    }

    // ---------- NEW METHODS ----------

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getInventorySummary() {
        InventorySummaryResponse summary = InventorySummaryResponse.builder()
                .totalProducts(inventoryRepository.countDistinctProductsWithInventory())
                .totalStock(inventoryRepository.sumTotalStock())
                .lowStock(inventoryRepository.countLowStock())
                .outOfStock(inventoryRepository.countOutOfStock())
                .build();
        return ResponseErrorTemplate.success("Inventory summary retrieved", summary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseErrorTemplate> searchInventory(String search, String warehouse, String status, Pageable pageable) {
        return inventoryRepository.searchInventory(search, warehouse, status, pageable)
                .map(InventoryMapper::toWrappedResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getInventoryHistory(Long inventoryId, Pageable pageable) {
        if (!inventoryRepository.existsById(inventoryId)) {
            throw new ResourceNotFoundException("Inventory not found with id: " + inventoryId);
        }
        Page<StockMovement> page = stockMovementRepository.findByInventoryIdOrderByCreatedAtDesc(inventoryId, pageable);
        return ResponseErrorTemplate.success("History retrieved", page);
    }

    // Helper: clear existing default SKU (kept from original)
    private void clearExistingDefaultSku(Long productId) {
        inventoryRepository.findDefaultSkuByProductId(productId)
                .ifPresent(defaultSku -> {
                    defaultSku.setIsDefault(false);
                    productSkuRepository.save(defaultSku);
                });
    }
}