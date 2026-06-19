package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.ProductAttributeValueRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface ProductAttributeValueService {

    ResponseErrorTemplate createAttributeValue(Long id,ProductAttributeValueRequest request);

    ResponseErrorTemplate getAttributeValueById(Long id);

    List<ResponseErrorTemplate> getValuesByAttributeId(Long attributeId);

    ResponseErrorTemplate updateAttributeValue(Long id, ProductAttributeValueRequest request);

    void deleteAttributeValue(Long id);

    ResponseErrorTemplate getAttributeValueByAttributeAndValue(Long attributeId, String value);


}


