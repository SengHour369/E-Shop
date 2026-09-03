package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.DuplicateResourceException;
import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.Category;
import com.example.learning_spring_security.Model.CategoryIcon;
import com.example.learning_spring_security.Model.Image;
import com.example.learning_spring_security.Repository.CategoryRepository;
import com.example.learning_spring_security.Service.ServiceStructure.CategoryService;
import com.example.learning_spring_security.Service.ServiceStructure.ImageService;
import com.example.learning_spring_security.ServiceMapper.CategoryMapper;
import com.example.learning_spring_security.dto.Request.CategoryRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ImageService imageService;
    private final com.example.learning_spring_security.Repository.CategoryIconRepository categoryIconRepository;

    @Override
    @org.springframework.cache.annotation.CacheEvict(value = "categories", allEntries = true)
    public ResponseErrorTemplate createCategory(CategoryRequest request) {
        if (categoryRepository.existsByNameAndDeletedFalse(request.getName())) {
            throw new DuplicateResourceException("Category already exists with name: " + request.getName());
        }

        Category category = CategoryMapper.toEntity(request);
        if (request.getIconId() != null) {
            com.example.learning_spring_security.Model.CategoryIcon icon = categoryIconRepository.findById(request.getIconId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category icon not found with id: " + request.getIconId()));
            category.setIcon(icon.getUrl());
        }
        Category savedCategory = categoryRepository.save(category);
        return CategoryMapper.toResponse(savedCategory);
    }

    @Override
    @Cacheable(value = "categories", key = "#id")
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getCategoryById(Long id) {
        Category category = categoryRepository.findByCategoryId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return CategoryMapper.toResponse(category);
    }

    @Override
    @Cacheable(value = "categories", key = "'name:' + #name")
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getCategoryByName(String name) {
        Category category = categoryRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with name: " + name));
        return CategoryMapper.toResponse(category);
    }

    @Override
    @Cacheable(value = "categories", key = "'page:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<ResponseErrorTemplate> getAllCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable)
                .map(CategoryMapper::toResponse);
    }

    @Override
    @Cacheable(value = "categories", key = "'allActive'")
    @Transactional(readOnly = true)
    public List<ResponseErrorTemplate> getAllCategories() {
        return categoryRepository.findAllActive().stream()
                .map(CategoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @org.springframework.cache.annotation.CacheEvict(value = "categories", allEntries = true)
    public ResponseErrorTemplate updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findByCategoryId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (request.getName() != null && !request.getName().equals(category.getName())) {
            if (categoryRepository.existsByNameAndDeletedFalse(request.getName())) {
                throw new DuplicateResourceException("Category already exists with name: " + request.getName());
            }
        }

        CategoryMapper.updateEntity(category, request);
        if (request.getIconId() != null) {
            CategoryIcon icon = categoryIconRepository.findById(request.getIconId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category icon not found with id: " + request.getIconId()));
            category.setIcon(icon.getUrl());
        }
        Category updatedCategory = categoryRepository.save(category);
        return CategoryMapper.toResponse(updatedCategory);
    }

    @Override
    @org.springframework.cache.annotation.CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long id) {
        Optional<Category> category = categoryRepository.findByCategoryId(id);
        if (category.isEmpty()) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        CategoryIcon icon = categoryIconRepository.findById(category.get().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category icon not found with id: " + category.get().getId()));
        category.get().setDeleted(true);
        this.categoryIconRepository.deleteById(icon.getId());
        categoryRepository.save(category.get());
    }

    @Override
    @Cacheable(value = "categories", key = "'withSub:' + #id")
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getCategoryWithSubCategories(Long id) {
        Category category = categoryRepository.findByIdWithSubCategories(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return CategoryMapper.toResponseWithSubCategory(category);
    }

    @Override
    @Cacheable(value = "categories", key = "'allWithSub'")
    @Transactional(readOnly = true)
    public List<ResponseErrorTemplate> getAllCategoriesWithSubCategories() {
        return categoryRepository.findAllWithSubCategories().stream()
                .map(CategoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ResponseErrorTemplate uploadCategoryIcon(Long id, MultipartFile file) {
        Category category = categoryRepository.findByCategoryId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        Image uploaded = imageService.uploadImage(file);
        if (uploaded == null || uploaded.getUrl() == null) {
            throw new RuntimeException("Failed to upload icon image");
        }

        category.setIcon(uploaded.getUrl());
        Category saved = categoryRepository.save(category);
        return CategoryMapper.toResponse(saved);
    }
}