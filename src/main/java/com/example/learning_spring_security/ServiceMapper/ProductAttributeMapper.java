package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Model.ProductAttribute;
import com.example.learning_spring_security.dto.Response.ProductAttributeResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;

public class ProductAttributeMapper {

    public static ProductAttribute toEntity(String name) {
        return ProductAttribute.builder()
                .name(name)
                .build();
    }

    public static ResponseErrorTemplate toResponse(ProductAttribute attribute) {

        ProductAttributeResponse response = ProductAttributeResponse.builder()
                .id(attribute.getId())
                .name(attribute.getName())
                .build();
        return new ResponseErrorTemplate(Constant.SUC_MSG, Constant.SUC_CODE, response);
    }

    public static ProductAttributeResponse toResponseDTO(ProductAttribute attribute) {
        return ProductAttributeResponse.builder()
                .id(attribute.getId())
                .name(attribute.getName())
                .build();
    }

    public static void updateEntity(ProductAttribute attribute, String name) {
        attribute.setName(name);
    }
}

