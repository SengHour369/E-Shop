package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.Model.ProductSku;
import com.example.learning_spring_security.dto.Request.ProductSkuRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductSkuService {

    ProductSku createSku(Long productId, ProductSkuRequest request, MultipartFile image);

    ProductSku updateSku(Long skuId, ProductSkuRequest request, MultipartFile image);

    void deleteSku(Long skuId);

    ProductSku getSkuById(Long skuId);

    List<ProductSku> getSkusByProductId(Long productId);



}