package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.DuplicateResourceException;
import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.Category;
import com.example.learning_spring_security.Model.Image;
import com.example.learning_spring_security.Model.SubCategory;
import com.example.learning_spring_security.Repository.CategoryRepository;
import com.example.learning_spring_security.Repository.SubCategoryRepository;
import com.example.learning_spring_security.Service.ServiceStructure.ImageService;
import com.example.learning_spring_security.Service.ServiceStructure.SubCategoryService;
import com.example.learning_spring_security.ServiceMapper.SubCategoryMapper;
import com.example.learning_spring_security.dto.Request.GetSubCategoryRequest;
import com.example.learning_spring_security.dto.Request.SubCategoryRequest;
import com.example.learning_spring_security.dto.Response.SubCategoryPageResponse;
import com.example.learning_spring_security.dto.Response.SubCategoryResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SubCategoryServiceImpl implements SubCategoryService {

    private final SubCategoryRepository subCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final ImageService imageService;

    @Override
    public ResponseErrorTemplate createSubCategory(
            SubCategoryRequest request,
            MultipartFile file) throws Exception {

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        if (subCategoryRepository.existsByNameAndCategoryId(request.getName(), request.getCategoryId())) {
            throw new DuplicateResourceException("SubCategory already exists with name: " + request.getName() + " in this category");
        }

        if(file.isEmpty()) {
            throw new Exception("file in image is empty");
        }

        SubCategory subCategory = SubCategoryMapper.toEntity(request);
        subCategory.setCategory(category);

        Image imageUrl = this.imageService.uploadImage(file);
        subCategory.setImage(imageUrl);

        SubCategory savedSubCategory = subCategoryRepository.save(subCategory);
        return SubCategoryMapper.toResponse(savedSubCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getSubCategories(GetSubCategoryRequest request) {
        log.info("getSubCategories: criteriaType={}, criteriaValue={}, page={}, size={}",
                request.getCriteriaType(), request.getCriteriaValue(),
                request.getPage(), request.getSize());

        Pageable pageable = PageRequest.of(
                request.getPage() - 1,
                request.getSize(),
                Sort.by("name").ascending()
        );

        Integer type = request.getCriteriaType();
        String value = request.getCriteriaValue();

        // ── Single-item lookups ──────────────────────────────────────
        if (type == 4) { // by ID
            SubCategory sub = subCategoryRepository.findById(Long.parseLong(value))
                    .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + value));
            return SubCategoryMapper.toResponse(sub);
        }

        if (type == 5) { // by ID with products
            SubCategory sub = subCategoryRepository.findByIdWithProducts(Long.parseLong(value))
                    .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + value));
            return SubCategoryMapper.toResponseWithProducts(sub);
        }

        // ── Paginated lookups ────────────────────────────────────────
        Page<SubCategory> page;
        String successMsg;

        if (type == null || type == 0 || value == null || value.isBlank()) {
            page = subCategoryRepository.findAll(pageable);
            successMsg = "Retrieved all sub-categories";
        } else if (type == 1) { // by name (fuzzy)
            page = subCategoryRepository.findByNameContaining(value, pageable);
            successMsg = "Retrieved sub-categories by name";
        } else if (type == 2) { // by categoryId
            page = subCategoryRepository.findByCategoryId(Long.parseLong(value), pageable);
            successMsg = "Retrieved sub-categories by category";
        } else if (type == 3) { // by categoryId — alias same as type 2 but returns paged
            page = subCategoryRepository.findByCategoryId(Long.parseLong(value), pageable);
            successMsg = "Retrieved sub-categories by category";
        } else {
            page = subCategoryRepository.findAll(pageable);
            successMsg = "Retrieved all sub-categories";
        }

        List<SubCategoryResponse> payload = page.getContent()
                .stream()
                .map(SubCategoryMapper::toSubCategoryResponse)
                .toList();

        SubCategoryPageResponse pageResponse = SubCategoryPageResponse.builder()
                .payload(payload)
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber() + 1)
                .pageSize(page.getSize())
                .build();

        String message = page.isEmpty() ? "No sub-categories found" : successMsg;
        return ResponseErrorTemplate.success(message, pageResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getSubCategoryById(Long id) {
        SubCategory subCategory = subCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + id));
        return SubCategoryMapper.toResponse(subCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseErrorTemplate> getSubCategoryAll(Pageable pageable) {
        return subCategoryRepository.findAll(pageable)
                .map(SubCategoryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseErrorTemplate> getSubCategoriesByCategoryAsList(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found with id: " + categoryId);
        }
        return subCategoryRepository.findByCategoryId(categoryId).stream()
                .map(SubCategoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ResponseErrorTemplate updateSubCategory(Long id, SubCategoryRequest request,MultipartFile file) {
        SubCategory subCategory = subCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + id));

        if (request.getName() != null && !request.getName().equals(subCategory.getName())) {
            Long categoryId = request.getCategoryId() != null ? request.getCategoryId() : subCategory.getCategory().getId();
            if (subCategoryRepository.existsByNameAndCategoryId(request.getName(), categoryId)) {
                throw new DuplicateResourceException("SubCategory already exists with name: " + request.getName() + " in this category");
            }
        }

        if (request.getCategoryId() != null && !request.getCategoryId().equals(subCategory.getCategory().getId())) {
            Category newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
            subCategory.setCategory(newCategory);
        }

        if(file != null && !Objects.equals(file.getOriginalFilename(), subCategory.getImage())) {
            Image imageUrl = this.imageService.uploadImage(file);
            subCategory.setImage(imageUrl);
        }

        SubCategoryMapper.updateEntity(subCategory, request);
        SubCategory updatedSubCategory = subCategoryRepository.save(subCategory);
        return SubCategoryMapper.toResponse(updatedSubCategory);
    }

    @Override
    public void deleteSubCategory(Long id) {
        if (!subCategoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("SubCategory not found with id: " + id);
        }
        subCategoryRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getSubCategoryWithProducts(Long id) {
        SubCategory subCategory = subCategoryRepository.findByIdWithProducts(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + id));
        return SubCategoryMapper.toResponse(subCategory);
    }
}