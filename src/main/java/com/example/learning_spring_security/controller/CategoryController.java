package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.Service.ServiceStructure.CategoryService;
import com.example.learning_spring_security.dto.Request.CategoryRequest;
import com.example.learning_spring_security.dto.Response.CategoryResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
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
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController extends BaseController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<Page<ResponseErrorTemplate>> getAllCategories(
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<ResponseErrorTemplate> categories = categoryService.getAllCategories(pageable);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ResponseErrorTemplate>> getAllCategories() {
        List<ResponseErrorTemplate> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/with-subcategories")
    public ResponseEntity<List<ResponseErrorTemplate>> getAllCategoriesWithSubCategories() {
        List<ResponseErrorTemplate> categories = categoryService.getAllCategoriesWithSubCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseErrorTemplate> getCategoryById(@PathVariable Long id) {
        ResponseErrorTemplate category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<ResponseErrorTemplate> getCategoryByName(@PathVariable String name) {
        ResponseErrorTemplate category = categoryService.getCategoryByName(name);
        return ResponseEntity.ok(category);
    }

    @GetMapping("/{id}/with-subcategories")
    public ResponseEntity<ResponseErrorTemplate> getCategoryWithSubCategories(@PathVariable Long id) {
        ResponseErrorTemplate category = categoryService.getCategoryWithSubCategories(id);
        return ResponseEntity.ok(category);
    }

    @PostMapping
    public ResponseEntity<ResponseErrorTemplate> createCategory(@Valid @RequestBody CategoryRequest request) {
        ResponseErrorTemplate response = categoryService.createCategory(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseErrorTemplate> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        ResponseErrorTemplate response = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}