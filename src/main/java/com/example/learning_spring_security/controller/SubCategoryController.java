package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.Service.ServiceStructure.SubCategoryService;
import com.example.learning_spring_security.dto.Request.SubCategoryRequest;
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
    public ResponseEntity<Page<SubCategoryResponse>> getSubCategoriesByCategory(
            @PathVariable Long categoryId,
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<SubCategoryResponse> subCategories = subCategoryService.getSubCategoriesByCategory(categoryId, pageable);
        return ResponseEntity.ok(subCategories);
    }

    @GetMapping("/category/{categoryId}/all")
    public ResponseEntity<List<SubCategoryResponse>> getSubCategoriesByCategoryAsList(@PathVariable Long categoryId) {
        List<SubCategoryResponse> subCategories = subCategoryService.getSubCategoriesByCategoryAsList(categoryId);
        return ResponseEntity.ok(subCategories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubCategoryResponse> getSubCategoryById(@PathVariable Long id) {
        SubCategoryResponse subCategory = subCategoryService.getSubCategoryById(id);
        return ResponseEntity.ok(subCategory);
    }

    @GetMapping("/{id}/with-products")
    public ResponseEntity<SubCategoryResponse> getSubCategoryWithProducts(@PathVariable Long id) {
        SubCategoryResponse subCategory = subCategoryService.getSubCategoryWithProducts(id);
        return ResponseEntity.ok(subCategory);
    }

    @PostMapping
    public ResponseEntity<SubCategoryResponse> createSubCategory(@Valid @RequestBody SubCategoryRequest request) {
        SubCategoryResponse response = subCategoryService.createSubCategory(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubCategoryResponse> updateSubCategory(
            @PathVariable Long id,
            @Valid @RequestBody SubCategoryRequest request) {
        SubCategoryResponse response = subCategoryService.updateSubCategory(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubCategory(@PathVariable Long id) {
        subCategoryService.deleteSubCategory(id);
        return ResponseEntity.noContent().build();
    }
}