package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.GetSubCategoryRequest;
import com.example.learning_spring_security.dto.Request.SubCategoryRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface SubCategoryService {

    // Paginated – skip
    ResponseErrorTemplate getSubCategories(GetSubCategoryRequest request);

    @CacheEvict(value = "subCategories", allEntries = true)
    ResponseErrorTemplate createSubCategory(SubCategoryRequest request, MultipartFile file) throws Exception;

    @CacheEvict(value = "subCategories", allEntries = true)
    ResponseErrorTemplate updateSubCategory(Long id, SubCategoryRequest request, MultipartFile file);

    @CacheEvict(value = "subCategories", allEntries = true)
    void deleteSubCategory(Long id);

    @Cacheable(value = "subCategories", key = "#id")
    ResponseErrorTemplate getSubCategoryById(Long id);

    @Cacheable(value = "subCategories", key = "#id + ':withProducts'")
    ResponseErrorTemplate getSubCategoryWithProducts(Long id);

    // Paginated – skip
    Page<ResponseErrorTemplate> getSubCategoryAll(Pageable pageable);

    @Cacheable(value = "subCategories", key = "#categoryId + ':categoryList'")
    List<ResponseErrorTemplate> getSubCategoriesByCategoryAsList(Long categoryId);
}