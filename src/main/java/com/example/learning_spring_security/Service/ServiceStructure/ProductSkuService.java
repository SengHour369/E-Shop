package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.Model.ProductSku;
import com.example.learning_spring_security.dto.Request.ProductSkuRequest;

import java.util.List;

public interface ProductSkuService {

    ProductSku createSku(Long productId, ProductSkuRequest request);

    ProductSku updateSku(Long skuId, ProductSkuRequest request);

    void deleteSku(Long skuId);

    ProductSku getSkuById(Long skuId);

    List<ProductSku> getSkusByProductId(Long productId);



}