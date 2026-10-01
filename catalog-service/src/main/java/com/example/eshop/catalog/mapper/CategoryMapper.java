package com.example.eshop.catalog.mapper;

import com.example.eshop.catalog.constant.Constant;
import com.example.eshop.catalog.model.Category;
import com.example.eshop.catalog.dto.request.CategoryRequest;
import com.example.eshop.catalog.dto.response.CategoryResponse;
import com.example.eshop.catalog.dto.response.CategoryResponseWithSubCategory;
import com.example.eshop.catalog.dto.response.ResponseErrorTemplate;


public class CategoryMapper {

    public static Category toEntity(CategoryRequest request) {
        return Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .status(request.getStatus())
                .build();
    }

    public static ResponseErrorTemplate toResponse(Category category) {
        CategoryResponse categoryResponse = CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .icon(category.getIcon())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
        return new ResponseErrorTemplate(Constant.SUC_MSG, Constant.SUC_CODE, categoryResponse);
    }

    public static void updateEntity(Category category, CategoryRequest request) {
        category.setName(request.getName());
        category.setDescription(request.getDescription());
    }
    public static ResponseErrorTemplate toResponseWithSubCategory(Category category) {
        CategoryResponseWithSubCategory categoryResponse = CategoryResponseWithSubCategory.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .subCategories(category.getSubCategories().
                        stream()
                        .map(SubCategoryMapper::toResponse).toList())
                .build();
        return new ResponseErrorTemplate(Constant.SUC_MSG, Constant.SUC_CODE, categoryResponse);
    }
}