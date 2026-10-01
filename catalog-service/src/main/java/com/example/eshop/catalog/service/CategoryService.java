package com.example.eshop.catalog.service;

import com.example.eshop.catalog.dto.request.CategoryRequest;
import com.example.eshop.catalog.dto.response.CategoryResponse;
import com.example.eshop.catalog.dto.response.ResponseErrorTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

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
    ResponseErrorTemplate uploadCategoryIcon(Long id, MultipartFile file);
}