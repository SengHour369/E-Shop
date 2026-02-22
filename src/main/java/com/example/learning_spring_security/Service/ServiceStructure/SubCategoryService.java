package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.SubCategoryRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import com.example.learning_spring_security.dto.Response.SubCategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SubCategoryService {
    ResponseErrorTemplate createSubCategory(SubCategoryRequest request);
    ResponseErrorTemplate getSubCategoryById(Long id);


    Page<ResponseErrorTemplate> getSubCategoriesByCategory(Long categoryId, Pageable pageable);


    List<ResponseErrorTemplate> getSubCategoriesByCategoryAsList(Long categoryId);

    ResponseErrorTemplate updateSubCategory(Long id, SubCategoryRequest request);
    void deleteSubCategory(Long id);
    ResponseErrorTemplate getSubCategoryWithProducts(Long id);
}