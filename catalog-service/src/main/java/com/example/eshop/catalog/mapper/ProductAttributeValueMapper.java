package com.example.eshop.catalog.mapper;

import com.example.eshop.catalog.constant.Constant;
import com.example.eshop.catalog.model.ProductAttributeValue;
import com.example.eshop.catalog.dto.response.ProductAttributeValueResponse;
import com.example.eshop.catalog.dto.response.ResponseErrorTemplate;

public class ProductAttributeValueMapper {

    public static ProductAttributeValueResponse toResponse(ProductAttributeValue attributeValue) {
        ProductAttributeValueResponse response = ProductAttributeValueResponse.builder()
                .id(attributeValue.getId())
                .value(attributeValue.getValue())
                .build();
        return  response;
    }

    public static ProductAttributeValueResponse toResponseDTO(ProductAttributeValue attributeValue) {
        return ProductAttributeValueResponse.builder()
                .id(attributeValue.getId())
                .value(attributeValue.getValue())
                .build();
    }

    public static void updateEntity(ProductAttributeValue attributeValue, String value) {
        attributeValue.setValue(value);
    }
}
