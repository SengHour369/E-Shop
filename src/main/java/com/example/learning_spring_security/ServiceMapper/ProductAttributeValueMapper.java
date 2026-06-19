package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Model.ProductAttributeValue;
import com.example.learning_spring_security.dto.Response.ProductAttributeValueResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;

public class ProductAttributeValueMapper {

    public static ResponseErrorTemplate toResponse(ProductAttributeValue attributeValue) {
        ProductAttributeValueResponse response = ProductAttributeValueResponse.builder()
                .id(attributeValue.getId())
                .value(attributeValue.getValue())
                .attributeId(attributeValue.getAttribute().getId())
                .attributeName(attributeValue.getAttribute().getName())
                .build();
        return new ResponseErrorTemplate(Constant.SUC_MSG, Constant.SUC_CODE, response);
    }

    public static ProductAttributeValueResponse toResponseDTO(ProductAttributeValue attributeValue) {
        return ProductAttributeValueResponse.builder()
                .id(attributeValue.getId())
                .value(attributeValue.getValue())
                .attributeId(attributeValue.getAttribute().getId())
                .attributeName(attributeValue.getAttribute().getName())
                .build();
    }

    public static void updateEntity(ProductAttributeValue attributeValue, String value) {
        attributeValue.setValue(value);
    }
}

