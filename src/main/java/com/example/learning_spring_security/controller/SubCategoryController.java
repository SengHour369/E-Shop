package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.Service.ServiceStructure.SubCategoryService;
import com.example.learning_spring_security.dto.Request.GetSubCategoryRequest;
import com.example.learning_spring_security.dto.Request.SubCategoryRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/subcategories")
@RequiredArgsConstructor
public class SubCategoryController extends BaseController {

    private final SubCategoryService subCategoryService;

    @PostMapping("/get/all")
    public ResponseEntity<ResponseErrorTemplate> getSubCategories(
            @RequestBody GetSubCategoryRequest request) {
        ResponseErrorTemplate response = subCategoryService.getSubCategories(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/create/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping(value = "/update/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseErrorTemplate> updateSubCategory(
            @RequestParam Long id,
            @RequestParam String name,
            @RequestParam Long categoryId,
            @RequestParam String description,
            @RequestParam(required = false) MultipartFile file) {
        SubCategoryRequest request = new SubCategoryRequest();
        request.setCategoryId(categoryId);
        request.setName(name);
        request.setDescription(description);
        ResponseErrorTemplate response = subCategoryService.updateSubCategory(id, request, file);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/delete/")
    public ResponseEntity<ResponseErrorTemplate> deleteSubCategory(@RequestParam Long id) {
        subCategoryService.deleteSubCategory(id);
        return ResponseEntity.ok(ResponseErrorTemplate.success("SubCategory deleted successfully", null));
    }
}