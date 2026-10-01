package com.example.eshop.catalog.controller;

import com.example.eshop.catalog.service.impl.CategoryIconServiceImpl;
import com.example.eshop.catalog.service.CategoryIconService;
import com.example.eshop.catalog.dto.response.ResponseErrorTemplate;
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


}