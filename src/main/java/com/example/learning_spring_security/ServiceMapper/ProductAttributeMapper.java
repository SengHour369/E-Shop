package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Model.ProductAttribute;
import com.example.learning_spring_security.Model.ProductAttributeValue;
import com.example.learning_spring_security.Model.ProductSku;
import com.example.learning_spring_security.dto.Response.ProductAttributeResponse;
import com.example.learning_spring_security.dto.Response.ProductAttributeValueResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;

import java.util.List;

public class ProductAttributeMapper {

    public static ProductAttribute toEntity(String name, ProductSku productSku) {
        return ProductAttribute.builder()
                .name(name)
                .productSkuId(productSku.getId())
                .build();
    }

    public static ProductAttributeResponse toResponse(ProductAttribute attribute) {

        ProductAttributeResponse response = ProductAttributeResponse.builder()
                .id(attribute.getId())
                .name(attribute.getName())
                .build();
        return response;
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

