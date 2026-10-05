package com.example.eshop.catalog.controller;

import com.example.eshop.catalog.service.impl.BrandLogoServiceImpl;
import com.example.eshop.catalog.dto.response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/brand-logos")
@RequiredArgsConstructor
public class BrandLogoController extends BaseController {

    private final BrandLogoServiceImpl brandLogoService;

    @GetMapping("/get/all")
    public ResponseEntity<List<ResponseErrorTemplate>> getAllLogos() {
        return ResponseEntity.ok(brandLogoService.getAllLogos());
    }

    @GetMapping("/id")
    public ResponseEntity<ResponseErrorTemplate> getLogoById(@RequestParam Long id) {
        return ResponseEntity.ok(brandLogoService.getLogoById(id));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseErrorTemplate> uploadLogo(
            @RequestParam String name,
            @RequestParam("file") MultipartFile file) {
        ResponseErrorTemplate response = brandLogoService.uploadLogo(name, file);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

}
