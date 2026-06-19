package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.Model.Product;
import com.example.learning_spring_security.Service.ServiceStructure.ProductService;

import com.example.learning_spring_security.dto.Request.ProductRequest;
import com.example.learning_spring_security.dto.Response.ProductResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
@Tag(name = "product-controller", description = "Product management APIs")
public class ProductController extends BaseController {

    private final ProductService productService;

    @PostMapping("/get/all")
    @Operation(summary = "Get all products", description = "Retrieve all products with pagination")
    public ResponseEntity<Page<ResponseErrorTemplate>> getAllProducts(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ResponseErrorTemplate> products = productService.getAllProducts(pageable);
        return ResponseEntity.ok(products);
    }

    @PostMapping("/active")
    @Operation(summary = "Get active products", description = "Retrieve only active products")
    public ResponseEntity<Page<ResponseErrorTemplate>> getActiveProducts(
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<ResponseErrorTemplate> products = productService.getActiveProducts(pageable);
        return ResponseEntity.ok(products);
    }

    @PostMapping("/search")
    @Operation(summary = "Search products", description = "Search products by keyword")
    public ResponseEntity<Page<ResponseErrorTemplate>> searchProducts(
            @Parameter(description = "Search keyword", example = "phone")
            @RequestParam String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<ResponseErrorTemplate> products = productService.searchProducts(keyword, pageable);
        return ResponseEntity.ok(products);
    }

    @PostMapping("/subcategory/id")
    @Operation(summary = "Get products by subcategory", description = "Get products by subcategory ID")
    public ResponseEntity<Page<ResponseErrorTemplate>> getProductsBySubCategory(
            @Parameter(description = "SubCategory ID", example = "1")
            @RequestParam Long subCategoryId,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<ResponseErrorTemplate> products = productService.getProductsBySubCategory(subCategoryId, pageable);
        return ResponseEntity.ok(products);
    }

    @PostMapping("/category/id")
    @Operation(summary = "Get products by category", description = "Get products by category ID")
    public ResponseEntity<Page<ResponseErrorTemplate>> getProductsByCategory(
            @Parameter(description = "Category ID", example = "1")
            @RequestParam Long categoryId,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<ResponseErrorTemplate> products = productService.getProductsByCategory(categoryId, pageable);
        return ResponseEntity.ok(products);
    }

    @PostMapping("/id")
    @Operation(summary = "Get product by ID", description = "Get a specific product by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ResponseErrorTemplate> getProductById(
            @Parameter(description = "Product ID", example = "1")
            @RequestParam Long id) {
        ResponseErrorTemplate product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @PostMapping("/id/with-skus")
    @Operation(summary = "Get product with SKUs", description = "Get product details with all SKU variants")
    public ResponseEntity<ResponseErrorTemplate> getProductWithSkus(
            @Parameter(description = "Product ID", example = "1")
            @RequestParam Long id) {
        ResponseErrorTemplate product = productService.getProductWithSkus(id);
        return ResponseEntity.ok(product);
    }
//    @PostMapping(value = "/create/",consumes = MediaType.APPLICATION_JSON_VALUE)
//    @PreAuthorize("hasAuthority('ADMIN')")
//    @Operation(summary = "Create product", description = "Create a new product via JSON (Admin only)")
//    public ResponseEntity<ResponseErrorTemplate> createProductJson(
//            @Valid @RequestBody ProductRequest request) throws Exception {
//        ResponseErrorTemplate response = productService.createProduct(request, null);
//        return new ResponseEntity<>(response, HttpStatus.CREATED);
//    }

    @PostMapping(value = "/create",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Create product with files", description = "Create a new product with optional images (Admin only)")
    public ResponseEntity<ResponseErrorTemplate> createProduct(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "true") Boolean is_active,
            @RequestParam Long sub_category_id,
            @RequestParam(required = false) String skus,
         //  @RequestBody ProductRequest productRequest,
            @RequestParam(value = "files") List<MultipartFile> files) throws Exception {
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
        
        ResponseErrorTemplate response = productService.createProduct(request, files);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping(value = "/id", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Update product", description = "Update an existing product via JSON (Admin only)")
    public ResponseEntity<ResponseErrorTemplate> updateProductJson(
            @Parameter(description = "Product ID", example = "1") @RequestParam Long id,
            @Valid @RequestBody ProductRequest request) throws Exception {
        ResponseErrorTemplate response = productService.updateProduct(id, request, null);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Update product with files", description = "Update an existing product with optional files (Admin only)")
    public ResponseEntity<ResponseErrorTemplate> updateProduct(
            @Parameter(description = "Product ID", example = "1") @RequestParam Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "true") Boolean is_active,
            @RequestParam Long sub_category_id,
            @RequestParam(required = false) String skus,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) throws Exception {
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
        
        ResponseErrorTemplate response = productService.updateProduct(id, request, files);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/id/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update product status", description = "Activate or deactivate a product (Admin only)")
    public ResponseEntity<ResponseErrorTemplate> updateProductStatus(
            @Parameter(description = "Product ID", example = "1") @RequestParam Long id,
            @Parameter(description = "Active status", example = "true") @RequestParam Boolean isActive) {
        ResponseErrorTemplate response = productService.updateProductStatus(id, isActive);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/delete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete product", description = "Delete a product by ID (Admin only)")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product ID", example = "1") @RequestParam Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}