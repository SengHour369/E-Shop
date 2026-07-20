package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.ProductAttributeValueRequest;
import com.example.learning_spring_security.dto.Response.ProductAttributeValueResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
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


