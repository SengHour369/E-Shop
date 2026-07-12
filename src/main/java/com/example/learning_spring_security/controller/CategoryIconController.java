package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.Service.ServiceImplement.CategoryIconServiceImpl;
import com.example.learning_spring_security.Service.ServiceStructure.CategoryIconService;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/category-icons")
@RequiredArgsConstructor
public class CategoryIconController extends BaseController {

    private final CategoryIconServiceImpl categoryIconService;

    @GetMapping("/get/all")
    public ResponseEntity<List<ResponseErrorTemplate>> getAllIcons() {
        return ResponseEntity.ok(categoryIconService.getAllIcons());
    }

    @GetMapping("/id")
    public ResponseEntity<ResponseErrorTemplate> getIconById(@RequestParam Long id) {
        return ResponseEntity.ok(categoryIconService.getIconById(id));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseErrorTemplate> uploadIcon(
            @RequestParam String name,
            @RequestParam("file") MultipartFile file) {
        ResponseErrorTemplate response = categoryIconService.uploadIcon(name, file);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteIcon(@RequestParam Long id) {
        categoryIconService.deleteIcon(id);
        return ResponseEntity.noContent().build();
    }
}