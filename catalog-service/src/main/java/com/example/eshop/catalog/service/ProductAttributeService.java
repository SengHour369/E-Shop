package com.example.eshop.catalog.service;

import com.example.eshop.catalog.dto.request.ProductAttributeRequest;
import com.example.eshop.catalog.dto.response.ProductAttributeResponse;
import com.example.eshop.catalog.dto.response.ResponseErrorTemplate;

import java.util.List;

public interface ProductAttributeService {

    ProductAttributeResponse createAttribute(Long id, ProductAttributeRequest request);

    ProductAttributeResponse getAttributeById(Long id);

    ProductAttributeResponse getAttributeByName(String name);

    List<ProductAttributeResponse> getAllAttributes();

    ProductAttributeResponse updateAttribute(Long id, ProductAttributeRequest request);

    void deleteAttribute(Long id);
}


