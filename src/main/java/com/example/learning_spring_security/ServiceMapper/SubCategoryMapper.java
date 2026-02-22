package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Model.SubCategory;
import com.example.learning_spring_security.dto.Request.SubCategoryRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import com.example.learning_spring_security.dto.Response.SubCategoryResponse;

public class SubCategoryMapper {

    public static SubCategory toEntity(SubCategoryRequest request) {
        if (request == null) {
            return null;
        }

        return SubCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
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
                .build();

        if (subCategory.getCategory() != null) {
            response.setCategoryId(subCategory.getCategory().getId());
            response.setCategoryName(subCategory.getCategory().getName());
        }

        return new ResponseErrorTemplate(Constant.SUC_MSG, Constant.SUC_CODE, response);
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
    }
}