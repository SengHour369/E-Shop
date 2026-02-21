package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.SubCategoryRequest;
import com.example.learning_spring_security.dto.Response.SubCategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SubCategoryService {
    SubCategoryResponse createSubCategory(SubCategoryRequest request);
    SubCategoryResponse getSubCategoryById(Long id);

    // Method សម្រាប់ Pagination
    Page<SubCategoryResponse> getSubCategoriesByCategory(Long categoryId, Pageable pageable);

    // Method សម្រាប់ List ធម្មតា
    List<SubCategoryResponse> getSubCategoriesByCategoryAsList(Long categoryId);

    SubCategoryResponse updateSubCategory(Long id, SubCategoryRequest request);
    void deleteSubCategory(Long id);
    SubCategoryResponse getSubCategoryWithProducts(Long id);
}