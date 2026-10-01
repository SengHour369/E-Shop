package com.example.eshop.catalog.service;

import com.example.eshop.catalog.dto.request.ProductAttributeValueRequest;
import com.example.eshop.catalog.dto.response.ProductAttributeValueResponse;
import com.example.eshop.catalog.dto.response.ResponseErrorTemplate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface ProductAttributeValueService {

    ProductAttributeValueResponse createAttributeValue(Long id, ProductAttributeValueRequest request);

    ProductAttributeValueResponse getAttributeValueById(Long id);

    List<ProductAttributeValueResponse> getValuesByAttributeId(Long attributeId);

    ProductAttributeValueResponse updateAttributeValue(Long id, ProductAttributeValueRequest request);

    void deleteAttributeValue(Long id);

    ProductAttributeValueResponse getAttributeValueByAttributeAndValue(Long attributeId, String value);


}


