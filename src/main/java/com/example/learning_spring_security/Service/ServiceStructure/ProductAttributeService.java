package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.ProductAttributeRequest;
import com.example.learning_spring_security.dto.Response.ProductAttributeResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import java.util.List;

public interface ProductAttributeService {

    @CacheEvict(value = "attributes", allEntries = true)
    ProductAttributeResponse createAttribute(Long id, ProductAttributeRequest request);

    @Cacheable(value = "attributes", key = "#id")
    ProductAttributeResponse getAttributeById(Long id);

    @Cacheable(value = "attributes", key = "#name")
    ProductAttributeResponse getAttributeByName(String name);

    @Cacheable(value = "attributes", key = "'all'")
    List<ProductAttributeResponse> getAllAttributes();

    @CacheEvict(value = "attributes", allEntries = true)
    ProductAttributeResponse updateAttribute(Long id, ProductAttributeRequest request);

    @CacheEvict(value = "attributes", allEntries = true)
    void deleteAttribute(Long id);
}