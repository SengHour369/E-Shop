package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.ProductAttributeRequest;
import com.example.learning_spring_security.dto.Response.ProductAttributeResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;

import java.util.List;

public interface ProductAttributeService {

    ProductAttributeResponse createAttribute(Long id, ProductAttributeRequest request);

    ProductAttributeResponse getAttributeById(Long id);

    ProductAttributeResponse getAttributeByName(String name);

    List<ProductAttributeResponse> getAllAttributes();

    ProductAttributeResponse updateAttribute(Long id, ProductAttributeRequest request);

    void deleteAttribute(Long id);
}


