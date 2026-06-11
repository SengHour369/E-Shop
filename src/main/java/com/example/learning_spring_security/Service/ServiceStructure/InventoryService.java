package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.InventoryRequest;
import com.example.learning_spring_security.dto.Request.RestockRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryService {

    ResponseErrorTemplate createInventory(InventoryRequest request);

    ResponseErrorTemplate getInventoryById(Long id);

    ResponseErrorTemplate getInventoryBySkuId(Long skuId);

    Page<ResponseErrorTemplate> getAllInventory(Pageable pageable);

    Page<ResponseErrorTemplate> getInventoryByProductId(Long productId, Pageable pageable);

    Page<ResponseErrorTemplate> getLowStockInventory(Long threshold, Pageable pageable);

    ResponseErrorTemplate restock(Long id, RestockRequest request);

    ResponseErrorTemplate adjustQuantity(Long id, InventoryRequest request);

    void deleteInventory(Long id);
}