package com.example.eshop.catalog.controller;

import com.example.eshop.catalog.service.ProductService;
import com.example.eshop.catalog.dto.request.GetProductRequest;
import com.example.eshop.catalog.dto.request.ProductRequest;
import com.example.eshop.catalog.dto.response.ResponseErrorTemplate;
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
    private final ObjectMapper objectMapper;

    @PostMapping("/get/all")
    public ResponseEntity<ResponseErrorTemplate> getProducts(
            @Valid @RequestBody GetProductRequest request) {
        ResponseErrorTemplate response = productService.getProducts(request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @PostMapping(value = "/create/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseErrorTemplate> createProduct(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "true") Boolean is_active,
            @RequestParam Long sub_category_id,
            @RequestParam(required = false) String skus,
            @RequestParam(value = "files") List<MultipartFile> files,
            @RequestParam(value = "sku_images", required = false) List<MultipartFile> sku_images) throws Exception {
        ObjectMapper mapper = objectMapper;
        ProductRequest request = new ProductRequest();
        request.setName(name);
        request.setDescription(description);
        request.setIsActive(is_active);
        request.setSubCategoryId(sub_category_id);

        if (skus != null && !skus.isEmpty()) {
            List<com.example.eshop.catalog.dto.request.ProductSkuRequest> skuList =
                    mapper.readValue(skus, mapper.getTypeFactory().constructCollectionType(
                            List.class, com.example.eshop.catalog.dto.request.ProductSkuRequest.class));

            request.setSkus(skuList);
        }

        ResponseErrorTemplate response = productService.createProduct(request, files, sku_images);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @PutMapping(value = "/update/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseErrorTemplate> updateProduct(
            @RequestParam Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "true") Boolean is_active,
            @RequestParam Long sub_category_id,
            @RequestParam(required = false) String skus,
            @RequestParam(value = "files") List<MultipartFile> files,
            @RequestParam(value = "sku_images", required = false) List<MultipartFile> sku_images) throws Exception {
        ObjectMapper mapper = objectMapper;
        ProductRequest request = new ProductRequest();
        request.setName(name);
        request.setDescription(description);
        request.setIsActive(is_active);
        request.setSubCategoryId(sub_category_id);

        if (skus != null && !skus.isEmpty()) {
            List<com.example.eshop.catalog.dto.request.ProductSkuRequest> skuList =
                    mapper.readValue(skus, mapper.getTypeFactory().constructCollectionType(
                            List.class, com.example.eshop.catalog.dto.request.ProductSkuRequest.class));

            request.setSkus(skuList);
        }
        ResponseErrorTemplate response = productService.updateProduct(id, request, files, sku_images);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @PutMapping(value = "/update/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseErrorTemplate> updateProductJson(
            @RequestParam Long id,
            @Valid @RequestBody ProductRequest request) throws Exception {
        ResponseErrorTemplate response = productService.updateProduct(id, request, null, null);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @PostMapping("/update/status/")
    public ResponseEntity<ResponseErrorTemplate> updateProductStatus(
            @RequestParam Long id,
            @RequestParam Boolean isActive) {
        ResponseErrorTemplate response = productService.updateProductStatus(id, isActive);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/delete/")
    public ResponseEntity<ResponseErrorTemplate> deleteProduct(@RequestParam Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ResponseErrorTemplate.success("Product deleted successfully", null));
    }
}
