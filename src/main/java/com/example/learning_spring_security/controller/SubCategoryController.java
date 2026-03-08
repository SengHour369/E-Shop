package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.Service.ServiceStructure.SubCategoryService;
import com.example.learning_spring_security.dto.Request.ProductRequest;
import com.example.learning_spring_security.dto.Request.SubCategoryRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subcategories")
@RequiredArgsConstructor
public class SubCategoryController extends BaseController {

    private final SubCategoryService subCategoryService;

    @GetMapping("/All")
    public ResponseEntity<Page<ResponseErrorTemplate>> getSubCategoriesByCategory(
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<ResponseErrorTemplate> subCategories = subCategoryService.getSubCategoryAll( pageable);
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
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseErrorTemplate> createSubCategory(
            @RequestParam String name,
            @RequestParam Long categoryId,
            @RequestParam String description,
            @RequestParam(required = false) MultipartFile image) throws Exception {

        SubCategoryRequest request = new SubCategoryRequest();
        request.setName(name);
        request.setCategoryId(categoryId);
        request.setDescription(description);

        ResponseErrorTemplate response = subCategoryService.createSubCategory(request, image);
        return ResponseEntity.ok(response);
    }
    @PutMapping(value = "/{id}",consumes =  MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseErrorTemplate> updateSubCategory(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam Long categoryId,
            @RequestParam String description,
            @RequestParam(required = false) MultipartFile file) {
        SubCategoryRequest request = new SubCategoryRequest();
        request.setCategoryId(categoryId);
        request.setName(name);
        request.setDescription(description);
        ResponseErrorTemplate response = subCategoryService.updateSubCategory(id, request,file);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubCategory(@PathVariable Long id) {
        subCategoryService.deleteSubCategory(id);
        return ResponseEntity.noContent().build();
    }
}