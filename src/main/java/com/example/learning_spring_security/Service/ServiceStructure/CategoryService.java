package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.CategoryRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface CategoryService {

    @CacheEvict(value = "categories", allEntries = true)
    ResponseErrorTemplate createCategory(CategoryRequest request);

    @Cacheable(value = "categories", key = "#id")
    ResponseErrorTemplate getCategoryById(Long id);

    @Cacheable(value = "categories", key = "#name")
    ResponseErrorTemplate getCategoryByName(String name);

    // Paginated – skip
    Page<ResponseErrorTemplate> getAllCategories(Pageable pageable);

    @Cacheable(value = "categories", key = "'all'")
    List<ResponseErrorTemplate> getAllCategories();

    @CacheEvict(value = "categories", allEntries = true)
    ResponseErrorTemplate updateCategory(Long id, CategoryRequest request);

    @CacheEvict(value = "categories", allEntries = true)
    void deleteCategory(Long id);

    @Cacheable(value = "categories", key = "#id + ':withSub'")
    ResponseErrorTemplate getCategoryWithSubCategories(Long id);

    @Cacheable(value = "categories", key = "'allWithSub'")
    List<ResponseErrorTemplate> getAllCategoriesWithSubCategories();

    @CacheEvict(value = "categories", key = "#id")
    ResponseErrorTemplate uploadCategoryIcon(Long id, MultipartFile file);
}