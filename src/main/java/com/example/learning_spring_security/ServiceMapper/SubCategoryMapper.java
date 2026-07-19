package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Model.Product;
import com.example.learning_spring_security.Model.SubCategory;
import com.example.learning_spring_security.dto.Request.SubCategoryRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import com.example.learning_spring_security.dto.Response.SubCategoryResponse;

import java.util.List;
import java.util.stream.Collectors;

public class SubCategoryMapper {

    public static SubCategory toEntity(SubCategoryRequest request) {
        if (request == null) {
            return null;
        }

        return SubCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .status(request.getStatus())
                .deleted(false)
                .build();
    }

    public static ResponseErrorTemplate toResponse(SubCategory subCategory) {
        if (subCategory == null) {
            return null;
        }

        SubCategoryResponse response = SubCategoryResponse.builder()
                .id(subCategory.getId())
                .name(subCategory.getName())
                .description(subCategory.getDescription())
                .Status(subCategory.getStatus())
                .CategoryId(subCategory.getCategory() != null ? subCategory.getCategory().getId() : null)
                .createdAt(subCategory.getCreatedAt())
                .updatedAt(subCategory.getUpdatedAt())
                .build();

        if (subCategory.getCategory() != null) {
            response.setCategoryName(subCategory.getCategory().getName());
        }
        if (subCategory.getImage() != null) {
            response.setImage(subCategory.getImage().getUrl());
        }


        return new ResponseErrorTemplate(Constant.SUC_MSG, Constant.SUC_CODE, response);
    }

    public static SubCategoryResponse toSubCategoryResponse(SubCategory subCategory) {
        if (subCategory == null) return null;

        SubCategoryResponse response = SubCategoryResponse.builder()
                .id(subCategory.getId())
                .name(subCategory.getName())
                .description(subCategory.getDescription())
                .Status(true)
                .CategoryId(subCategory.getCategory() != null ? subCategory.getCategory().getId() : null)
                .createdAt(subCategory.getCreatedAt())
                .updatedAt(subCategory.getUpdatedAt())
                .build();

        if (subCategory.getCategory() != null) {
            response.setCategoryName(subCategory.getCategory().getName());
        }
        if (subCategory.getImage() != null) {
            response.setImage(subCategory.getImage().getUrl());
        }
        return response;
    }

    public static ResponseErrorTemplate toResponseWithProducts(SubCategory subCategory) {
        if (subCategory == null) return null;

        List<String> productNames = subCategory.getProducts() == null
                ? List.of()
                : subCategory.getProducts().stream()
                .map(Product::getName)
                .collect(Collectors.toList());

        SubCategoryResponse response = SubCategoryResponse.builder()
                .id(subCategory.getId())
                .name(subCategory.getName())
                .CategoryId(subCategory.getCategory() != null ? subCategory.getCategory().getId() : null)
                .description(subCategory.getDescription())
                .createdAt(subCategory.getCreatedAt())
                .updatedAt(subCategory.getUpdatedAt())
                .build();

        if (subCategory.getCategory() != null) {
            response.setCategoryName(subCategory.getCategory().getName());
        }
        if (subCategory.getImage() != null) {
            response.setImage(subCategory.getImage().getUrl());
        }

        return new ResponseErrorTemplate(Constant.SUC_MSG, Constant.SUC_CODE,
                new java.util.LinkedHashMap<>() {{
                    put("sub_category", response);
                    put("products", productNames);
                }});
    }

    public static void updateEntity(SubCategory subCategory, SubCategoryRequest request) {
        if (request == null || subCategory == null) {
            return;
        }

        if (request.getName() != null) {
            subCategory.setName(request.getName());
        }
        if (request.getDescription() != null) {
            subCategory.setDescription(request.getDescription());
        }
        if(subCategory.getImage() != null) {
            subCategory.setImage(subCategory.getImage());
        }
    }
}