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

    @PostMapping("/get/all")
    public ResponseEntity<Page<ResponseErrorTemplate>> getAllCategories(
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<ResponseErrorTemplate> categories = categoryService.getAllCategories(pageable);
        return ResponseEntity.ok(categories);
    }

    @PostMapping("/id/get/")
    public ResponseEntity<ResponseErrorTemplate> getCategoryById(@RequestParam Long id) {
        ResponseErrorTemplate category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }

    @PostMapping("/name/")
    public ResponseEntity<ResponseErrorTemplate> getCategoryByName(@RequestParam String name) {
        ResponseErrorTemplate category = categoryService.getCategoryByName(name);
        return ResponseEntity.ok(category);
    }

    @PostMapping("/with-subcategories")
    public ResponseEntity<ResponseErrorTemplate> getCategoryWithSubCategories(@RequestParam Long id) {
        ResponseErrorTemplate category = categoryService.getCategoryWithSubCategories(id);
        return ResponseEntity.ok(category);
    }

    @PostMapping("/create/")
    public ResponseEntity<ResponseErrorTemplate> createCategory(@Valid @RequestBody CategoryRequest request) {
        ResponseErrorTemplate response = categoryService.createCategory(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/id/update")
    public ResponseEntity<ResponseErrorTemplate> updateCategory(
            @RequestParam Long id,
            @Valid @RequestBody CategoryRequest request) {
        ResponseErrorTemplate response = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteCategory(@RequestParam Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}