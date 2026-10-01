package com.example.eshop.catalog.service.impl;

import com.example.eshop.common.exception.BusinessLogicException;
import com.example.eshop.common.exception.ResourceNotFoundException;
import com.example.eshop.catalog.model.Category;
import com.example.eshop.catalog.model.CategoryIcon;
import com.example.eshop.catalog.model.Image;
import com.example.eshop.catalog.repository.CategoryRepository;
import com.example.eshop.catalog.service.CategoryService;
import com.example.eshop.catalog.service.ImageService;
import com.example.eshop.catalog.mapper.CategoryMapper;
import com.example.eshop.catalog.dto.request.CategoryRequest;
import com.example.eshop.catalog.dto.response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import com.example.eshop.common.audit.*;
import static com.example.eshop.catalog.audit.CatalogAuditSnapshots.of;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {
    private final AuditLogService audit;

    private final CategoryRepository categoryRepository;
    private final ImageService imageService;
    private final com.example.eshop.catalog.repository.CategoryIconRepository categoryIconRepository;

    @Override
    public ResponseErrorTemplate createCategory(CategoryRequest request) {
        if (categoryRepository.existsByNameAndDeletedFalse(request.getName())) {
            throw new BusinessLogicException("Category already exists with name: " + request.getName());
        }

        Category category = CategoryMapper.toEntity(request);
        if (request.getIconId() != null) {
            com.example.eshop.catalog.model.CategoryIcon icon = categoryIconRepository.findById(request.getIconId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category icon not found with id: " + request.getIconId()));
            category.setIcon(icon.getUrl());
        }
        Category savedCategory = categoryRepository.save(category);
        audit.record(AuditEvent.success(AuditAction.CATEGORY_CREATE, "CATEGORY", savedCategory.getId(), null, of(savedCategory)));
        return CategoryMapper.toResponse(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getCategoryById(Long id) {
        Category category = categoryRepository.findByCategoryId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return CategoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getCategoryByName(String name) {
        Category category = categoryRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with name: " + name));
        return CategoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseErrorTemplate> getAllCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable)
                .map(CategoryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseErrorTemplate> getAllCategories() {
        return categoryRepository.findAllActive().stream()
                .map(CategoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ResponseErrorTemplate updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findByCategoryId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (request.getName() != null && !request.getName().equals(category.getName())) {
            if (categoryRepository.existsByNameAndDeletedFalse(request.getName())) {
                throw new BusinessLogicException("Category already exists with name: " + request.getName());
            }
        }

        var before = of(category);
        CategoryMapper.updateEntity(category, request);
        if (request.getIconId() != null) {
            CategoryIcon icon = categoryIconRepository.findById(request.getIconId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category icon not found with id: " + request.getIconId()));
            category.setIcon(icon.getUrl());
        }
        Category updatedCategory = categoryRepository.save(category);
        audit.record(AuditEvent.success(AuditAction.CATEGORY_UPDATE, "CATEGORY", id, before, of(updatedCategory)));
        return CategoryMapper.toResponse(updatedCategory);
    }

    @Override
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
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getCategoryWithSubCategories(Long id) {
        Category category = categoryRepository.findByIdWithSubCategories(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return CategoryMapper.toResponseWithSubCategory(category);
    }

    @Override
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