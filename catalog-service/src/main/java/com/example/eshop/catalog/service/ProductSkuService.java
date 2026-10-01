package com.example.eshop.catalog.service;

import com.example.eshop.catalog.model.ProductSku;
import com.example.eshop.catalog.dto.request.ProductSkuRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductSkuService {

    ProductSku createSku(Long productId, ProductSkuRequest request, MultipartFile image);

    ProductSku updateSku(Long skuId, ProductSkuRequest request, MultipartFile image);

    void deleteSku(Long skuId);

    ProductSku getSkuById(Long skuId);

    List<ProductSku> getSkusByProductId(Long productId);



}