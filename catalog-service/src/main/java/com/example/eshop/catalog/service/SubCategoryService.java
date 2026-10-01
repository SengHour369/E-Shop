package com.example.eshop.catalog.service;

import com.example.eshop.catalog.dto.request.GetSubCategoryRequest;
import com.example.eshop.catalog.dto.request.SubCategoryRequest;
import com.example.eshop.catalog.dto.response.ResponseErrorTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SubCategoryService {
    ResponseErrorTemplate getSubCategories(GetSubCategoryRequest request);
    ResponseErrorTemplate createSubCategory(SubCategoryRequest request, MultipartFile file) throws Exception;
    ResponseErrorTemplate updateSubCategory(Long id, SubCategoryRequest request, MultipartFile file);
    void deleteSubCategory(Long id);

    // kept for backward compat
    ResponseErrorTemplate getSubCategoryById(Long id);
    ResponseErrorTemplate getSubCategoryWithProducts(Long id);
    Page<ResponseErrorTemplate> getSubCategoryAll(Pageable pageable);
    List<ResponseErrorTemplate> getSubCategoriesByCategoryAsList(Long categoryId);
}