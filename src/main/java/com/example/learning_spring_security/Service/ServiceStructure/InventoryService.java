package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.Model.ProductSku;
import com.example.learning_spring_security.dto.Request.InventoryRequest;
import com.example.learning_spring_security.dto.Request.RestockRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InventoryService {

    ResponseErrorTemplate createInventory(Long productSkuId, InventoryRequest request);
    ResponseErrorTemplate getInventoryById(Long id);
    ResponseErrorTemplate getInventoryBySkuId(Long skuId);
    Page<ResponseErrorTemplate> getAllInventory(Pageable pageable);
    Page<ResponseErrorTemplate> getInventoryByProductId(Long productId, Pageable pageable);
    Page<ResponseErrorTemplate> getLowStockInventory(Long threshold, Pageable pageable);
    ResponseErrorTemplate restock(Long id, RestockRequest request);
    ResponseErrorTemplate adjustQuantity(Long id, InventoryRequest request);
    void deleteInventory(Long id);
    void reduceStock(Long skuId, Long quantity);
    void increaseStock(Long skuId, Long quantity);
    List<ProductSku> getLowStockSkus();
    List<ProductSku> getLowStockSkusByProductId(Long productId);

    // ---------- NEW METHODS ----------
    ResponseErrorTemplate getInventorySummary();
    Page<ResponseErrorTemplate> searchInventory(String search, String warehouse, String status, Pageable pageable);
    ResponseErrorTemplate getInventoryHistory(Long inventoryId, Pageable pageable);
}