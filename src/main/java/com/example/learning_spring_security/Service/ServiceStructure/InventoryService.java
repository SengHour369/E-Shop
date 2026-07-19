package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.Model.ProductSku;
import com.example.learning_spring_security.dto.Request.InventoryRequest;
import com.example.learning_spring_security.dto.Request.RestockRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface InventoryService {

    @CacheEvict(value = "inventory", allEntries = true)
    ResponseErrorTemplate createInventory(Long productSkuId, InventoryRequest request);

    @Cacheable(value = "inventory", key = "#id")
    ResponseErrorTemplate getInventoryById(Long id);

    @Cacheable(value = "inventory", key = "#skuId + ':sku'")
    ResponseErrorTemplate getInventoryBySkuId(Long skuId);

    // Paginated – skip
    Page<ResponseErrorTemplate> getAllInventory(Pageable pageable);

    // Paginated – skip
    Page<ResponseErrorTemplate> getInventoryByProductId(Long productId, Pageable pageable);

    // Paginated – skip (or cache with short TTL)
    Page<ResponseErrorTemplate> getLowStockInventory(Long threshold, Pageable pageable);

    @CacheEvict(value = "inventory", allEntries = true)
    ResponseErrorTemplate restock(Long id, RestockRequest request);

    @CacheEvict(value = "inventory", allEntries = true)
    ResponseErrorTemplate adjustQuantity(Long id, InventoryRequest request);

    @CacheEvict(value = "inventory", allEntries = true)
    void deleteInventory(Long id);

    @CacheEvict(value = "inventory", allEntries = true)
    void reduceStock(Long skuId, Long quantity);

    @CacheEvict(value = "inventory", allEntries = true)
    void increaseStock(Long skuId, Long quantity);

    // Not cached (or use short TTL)
    List<ProductSku> getLowStockSkus();

    List<ProductSku> getLowStockSkusByProductId(Long productId);

    @Cacheable(value = "inventory", key = "'summary'")
    ResponseErrorTemplate getInventorySummary();

    // Paginated/search – skip
    Page<ResponseErrorTemplate> searchInventory(String search, String warehouse, String status, Pageable pageable);

    // Paginated – skip
    ResponseErrorTemplate getInventoryHistory(Long inventoryId, Pageable pageable);
}