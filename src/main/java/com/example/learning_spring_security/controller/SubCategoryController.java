package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.Service.ServiceStructure.SubCategoryService;
import com.example.learning_spring_security.dto.Request.SubCategoryRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import com.example.learning_spring_security.dto.Response.SubCategoryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subcategories")
@RequiredArgsConstructor
public class SubCategoryController extends BaseController {

    private final SubCategoryService subCategoryService;

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<ResponseErrorTemplate>> getSubCategoriesByCategory(
            @PathVariable Long categoryId,
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<ResponseErrorTemplate> subCategories = subCategoryService.getSubCategoriesByCategory(categoryId, pageable);
        return ResponseEntity.ok(subCategories);
    }

    @GetMapping("/category/{categoryId}/all")
    public ResponseEntity<List<ResponseErrorTemplate>> getSubCategoriesByCategoryAsList(@PathVariable Long categoryId) {
        List<ResponseErrorTemplate> subCategories = subCategoryService.getSubCategoriesByCategoryAsList(categoryId);
        return ResponseEntity.ok(subCategories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseErrorTemplate> getSubCategoryById(@PathVariable Long id) {
        ResponseErrorTemplate subCategory = subCategoryService.getSubCategoryById(id);
        return ResponseEntity.ok(subCategory);
    }

    @GetMapping("/{id}/with-products")
    public ResponseEntity<ResponseErrorTemplate> getSubCategoryWithProducts(@PathVariable Long id) {
        ResponseErrorTemplate subCategory = subCategoryService.getSubCategoryWithProducts(id);
        return ResponseEntity.ok(subCategory);
    }

    @PostMapping
    public ResponseEntity<ResponseErrorTemplate> createSubCategory(@Valid @RequestBody SubCategoryRequest request) {
        ResponseErrorTemplate response = subCategoryService.createSubCategory(request);
        return  ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseErrorTemplate> updateSubCategory(
            @PathVariable Long id,
            @Valid @RequestBody SubCategoryRequest request) {
        ResponseErrorTemplate response = subCategoryService.updateSubCategory(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubCategory(@PathVariable Long id) {
        subCategoryService.deleteSubCategory(id);
        return ResponseEntity.noContent().build();
    }
}