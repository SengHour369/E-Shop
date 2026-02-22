package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.CategoryRequest;
import com.example.learning_spring_security.dto.Response.CategoryResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {
    ResponseErrorTemplate createCategory(CategoryRequest request);
    ResponseErrorTemplate getCategoryById(Long id);
    ResponseErrorTemplate getCategoryByName(String name);
    Page<ResponseErrorTemplate> getAllCategories(Pageable pageable);
    List<ResponseErrorTemplate> getAllCategories();
    ResponseErrorTemplate updateCategory(Long id, CategoryRequest request);
    void deleteCategory(Long id);
    ResponseErrorTemplate getCategoryWithSubCategories(Long id);
    List<ResponseErrorTemplate> getAllCategoriesWithSubCategories();
}