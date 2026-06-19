package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.ProductAttributeRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;

import java.util.List;

public interface ProductAttributeService {

    ResponseErrorTemplate createAttribute(Long id, ProductAttributeRequest request);

    ResponseErrorTemplate getAttributeById(Long id);

    ResponseErrorTemplate getAttributeByName(String name);

    List<ResponseErrorTemplate> getAllAttributes();

    ResponseErrorTemplate updateAttribute(Long id, ProductAttributeRequest request);

    void deleteAttribute(Long id);
}


