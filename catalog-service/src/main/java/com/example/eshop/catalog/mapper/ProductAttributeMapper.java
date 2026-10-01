package com.example.eshop.catalog.mapper;

import com.example.eshop.catalog.constant.Constant;
import com.example.eshop.catalog.model.ProductAttribute;
import com.example.eshop.catalog.model.ProductAttributeValue;
import com.example.eshop.catalog.model.ProductSku;
import com.example.eshop.catalog.dto.response.ProductAttributeResponse;
import com.example.eshop.catalog.dto.response.ProductAttributeValueResponse;
import com.example.eshop.catalog.dto.response.ResponseErrorTemplate;

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

