package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.Model.ProductSku;
import com.example.learning_spring_security.dto.Request.ProductSkuRequest;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ProductSkuService {

    @CacheEvict(value = {"skus", "products"}, allEntries = true)
    ProductSku createSku(Long productId, ProductSkuRequest request, MultipartFile image);

    @CacheEvict(value = {"skus", "products"}, allEntries = true)
    ProductSku updateSku(Long skuId, ProductSkuRequest request, MultipartFile image);

    @CacheEvict(value = {"skus", "products"}, allEntries = true)
    void deleteSku(Long skuId);

    @Cacheable(value = "skus", key = "#skuId")
    ProductSku getSkuById(Long skuId);

    @Cacheable(value = "skus", key = "#productId + ':productSkus'")
    List<ProductSku> getSkusByProductId(Long productId);
}