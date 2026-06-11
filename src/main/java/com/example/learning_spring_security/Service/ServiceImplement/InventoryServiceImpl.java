package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.DuplicateResourceException;
import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.Inventory;
import com.example.learning_spring_security.Model.ProductSku;
import com.example.learning_spring_security.Repository.InventoryRepository;
import com.example.learning_spring_security.Repository.ProductSkuRepository;
import com.example.learning_spring_security.Service.ServiceStructure.InventoryService;
import com.example.learning_spring_security.ServiceMapper.InventoryMapper;
import com.example.learning_spring_security.dto.Request.InventoryRequest;
import com.example.learning_spring_security.dto.Request.RestockRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductSkuRepository productSkuRepository;

    @Override
    @Transactional
    public ResponseErrorTemplate createInventory(InventoryRequest request) {
        if (inventoryRepository.existsByProductSkuId(request.getProductSkuId())) {
            throw new DuplicateResourceException("Inventory already exists for SKU id: " + request.getProductSkuId());
        }

        ProductSku productSku = productSkuRepository.findById(request.getProductSkuId())
                .orElseThrow(() -> new ResourceNotFoundException("ProductSku not found with id: " + request.getProductSkuId()));

        Inventory inventory = InventoryMapper.toEntity(request, productSku);
        inventory.setLastRestockedAt(LocalDateTime.now());
        Inventory saved = inventoryRepository.save(inventory);

        log.info("Inventory created for SKU id={}", request.getProductSkuId());
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

        inventory.setQuantity(inventory.getQuantity() + request.getQuantity());
        inventory.setLastRestockedAt(LocalDateTime.now());
        Inventory updated = inventoryRepository.save(inventory);

        log.info("Inventory restocked: id={}, added={}, total={}", id, request.getQuantity(), updated.getQuantity());
        return InventoryMapper.toWrappedResponse(updated);
    }

    @Override
    @Transactional
    public ResponseErrorTemplate adjustQuantity(Long id, InventoryRequest request) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + id));

        inventory.setQuantity(request.getQuantity());
        if (request.getWarehouseLocation() != null) {
            inventory.setWarehouseLocation(request.getWarehouseLocation());
        }
        Inventory updated = inventoryRepository.save(inventory);

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
}