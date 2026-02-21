package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Model.Category;
import com.example.learning_spring_security.dto.Request.CategoryRequest;
import com.example.learning_spring_security.dto.Response.CategoryResponse;
import com.example.learning_spring_security.dto.Response.SubCategoryResponse;
import java.util.stream.Collectors;

public class CategoryMapper {

    public static Category toEntity(CategoryRequest request) {
        return Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    public static CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }

    public static void updateEntity(Category category, CategoryRequest request) {
        category.setName(request.getName());
        category.setDescription(request.getDescription());
    }
}