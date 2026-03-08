package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.DuplicateResourceException;
import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.Category;
import com.example.learning_spring_security.Model.SubCategory;
import com.example.learning_spring_security.Repository.CategoryRepository;
import com.example.learning_spring_security.Repository.SubCategoryRepository;
import com.example.learning_spring_security.Service.ServiceStructure.ImageService;
import com.example.learning_spring_security.Service.ServiceStructure.SubCategoryService;
import com.example.learning_spring_security.ServiceMapper.SubCategoryMapper;
import com.example.learning_spring_security.dto.Request.SubCategoryRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
            String  imageUrl = this.imageService.uploadImage(file);
            subCategory.setImage(imageUrl);

        SubCategory savedSubCategory = subCategoryRepository.save(subCategory);
        return SubCategoryMapper.toResponse(savedSubCategory);
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
        if(file != null &&   !Objects.equals(file.getOriginalFilename(), subCategory.getImage())) {
            String  imageUrl = this.imageService.uploadImage(file);
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