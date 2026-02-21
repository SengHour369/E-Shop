package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.CategoryRequest;
import com.example.learning_spring_security.dto.Response.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse getCategoryById(Long id);
    CategoryResponse getCategoryByName(String name);
    Page<CategoryResponse> getAllCategories(Pageable pageable);
    List<CategoryResponse> getAllCategories();
    CategoryResponse updateCategory(Long id, CategoryRequest request);
    void deleteCategory(Long id);
    CategoryResponse getCategoryWithSubCategories(Long id);
    List<CategoryResponse> getAllCategoriesWithSubCategories();
}