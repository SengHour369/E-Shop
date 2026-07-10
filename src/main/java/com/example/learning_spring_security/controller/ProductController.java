package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.Service.ServiceStructure.ProductService;
import com.example.learning_spring_security.dto.Request.GetProductRequest;
import com.example.learning_spring_security.dto.Request.ProductRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController extends BaseController {

    private final ProductService productService;

    @PostMapping("/get/all")
    public ResponseEntity<ResponseErrorTemplate> getProducts(
            @RequestBody GetProductRequest request) {
        ResponseErrorTemplate response = productService.getProducts(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/create/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ResponseErrorTemplate> createProduct(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "true") Boolean is_active,
            @RequestParam Long sub_category_id,
            @RequestParam(required = false) String skus,
            @RequestParam(value = "files") List<MultipartFile> files,
            @RequestParam(value = "sku_images", required = false) List<MultipartFile> sku_images) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ProductRequest request = new ProductRequest();
        request.setName(name);
        request.setDescription(description);
        request.setIsActive(is_active);
        request.setSubCategoryId(sub_category_id);

        if (skus != null && !skus.isEmpty()) {
            List<com.example.learning_spring_security.dto.Request.ProductSkuRequest> skuList =
                    mapper.readValue(skus, mapper.getTypeFactory().constructCollectionType(
                            List.class, com.example.learning_spring_security.dto.Request.ProductSkuRequest.class));

            request.setSkus(skuList);
        }

        ResponseErrorTemplate response = productService.createProduct(request, files, sku_images);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping(value = "/update/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ResponseErrorTemplate> updateProduct(
            @RequestParam Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "true") Boolean is_active,
            @RequestParam Long sub_category_id,
            @RequestParam(required = false) String skus,
            @RequestParam(value = "files") List<MultipartFile> files,
            @RequestParam(value = "sku_images", required = false) List<MultipartFile> sku_images) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ProductRequest request = new ProductRequest();
        request.setName(name);
        request.setDescription(description);
        request.setIsActive(is_active);
        request.setSubCategoryId(sub_category_id);

        if (skus != null && !skus.isEmpty()) {
            List<com.example.learning_spring_security.dto.Request.ProductSkuRequest> skuList =
                    mapper.readValue(skus, mapper.getTypeFactory().constructCollectionType(
                            List.class, com.example.learning_spring_security.dto.Request.ProductSkuRequest.class));

            request.setSkus(skuList);
        }
        ResponseErrorTemplate response = productService.updateProduct(id, request, files, sku_images);
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/update/", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ResponseErrorTemplate> updateProductJson(
            @RequestParam Long id,
            @Valid @RequestBody ProductRequest request) throws Exception {
        ResponseErrorTemplate response = productService.updateProduct(id, request, null, null);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/update/status/")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ResponseErrorTemplate> updateProductStatus(
            @RequestParam Long id,
            @RequestParam Boolean isActive) {
        ResponseErrorTemplate response = productService.updateProductStatus(id, isActive);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/delete/")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ResponseErrorTemplate> deleteProduct(@RequestParam Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ResponseErrorTemplate.success("Product deleted successfully", null));
    }
}