package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.ProductAttributeValueRequest;
import com.example.learning_spring_security.dto.Response.ProductAttributeValueResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

public interface ProductAttributeValueService {

    @CacheEvict(value = "attributeValues", allEntries = true)
    ProductAttributeValueResponse createAttributeValue(Long id, ProductAttributeValueRequest request);

    @Cacheable(value = "attributeValues", key = "#id")
    ProductAttributeValueResponse getAttributeValueById(Long id);

    @Cacheable(value = "attributeValues", key = "#attributeId + ':values'")
    List<ProductAttributeValueResponse> getValuesByAttributeId(Long attributeId);

    @CacheEvict(value = "attributeValues", allEntries = true)
    ProductAttributeValueResponse updateAttributeValue(Long id, ProductAttributeValueRequest request);

    @CacheEvict(value = "attributeValues", allEntries = true)
    void deleteAttributeValue(Long id);

    @Cacheable(value = "attributeValues", key = "#attributeId + ':' + #value")
    ProductAttributeValueResponse getAttributeValueByAttributeAndValue(Long attributeId, String value);
}