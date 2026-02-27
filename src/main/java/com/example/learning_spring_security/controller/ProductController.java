package com.example.learning_spring_security.controller;

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


@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "product-controller", description = "Product management APIs")
public class ProductController extends BaseController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Get all products", description = "Retrieve all products with pagination")
    public ResponseEntity<Page<ResponseErrorTemplate>> getAllProducts(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ResponseErrorTemplate> products = productService.getAllProducts(pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/active")
    @Operation(summary = "Get active products", description = "Retrieve only active products")
    public ResponseEntity<Page<ResponseErrorTemplate>> getActiveProducts(
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<ResponseErrorTemplate> products = productService.getActiveProducts(pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/search")
    @Operation(summary = "Search products", description = "Search products by keyword")
    public ResponseEntity<Page<ResponseErrorTemplate>> searchProducts(
            @Parameter(description = "Search keyword", example = "phone")
            @RequestParam String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<ResponseErrorTemplate> products = productService.searchProducts(keyword, pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/subcategory/{subCategoryId}")
    @Operation(summary = "Get products by subcategory", description = "Get products by subcategory ID")
    public ResponseEntity<Page<ResponseErrorTemplate>> getProductsBySubCategory(
            @Parameter(description = "SubCategory ID", example = "1")
            @PathVariable Long subCategoryId,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<ResponseErrorTemplate> products = productService.getProductsBySubCategory(subCategoryId, pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get products by category", description = "Get products by category ID")
    public ResponseEntity<Page<ResponseErrorTemplate>> getProductsByCategory(
            @Parameter(description = "Category ID", example = "1")
            @PathVariable Long categoryId,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<ResponseErrorTemplate> products = productService.getProductsByCategory(categoryId, pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Get a specific product by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ResponseErrorTemplate> getProductById(
            @Parameter(description = "Product ID", example = "1")
            @PathVariable Long id) {
        ResponseErrorTemplate product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/{id}/with-skus")
    @Operation(summary = "Get product with SKUs", description = "Get product details with all SKU variants")
    public ResponseEntity<ResponseErrorTemplate> getProductWithSkus(
            @Parameter(description = "Product ID", example = "1")
            @PathVariable Long id) {
        ResponseErrorTemplate product = productService.getProductWithSkus(id);
        return ResponseEntity.ok(product);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create product", description = "Create a new product (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin only")
    })
    public ResponseEntity<ResponseErrorTemplate> createProduct(@Valid @RequestBody ProductRequest request) {
        ResponseErrorTemplate response = productService.createProduct(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Add image to product for admin", description = "Upload an image to an existing product")
    public ResponseEntity<ResponseErrorTemplate> addProductImage(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file) {

        ResponseErrorTemplate response = productService.addImageToProduct(id, file);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update product", description = "Update an existing product (Admin only)")
    public ResponseEntity<ResponseErrorTemplate> updateProduct(
            @Parameter(description = "Product ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        ResponseErrorTemplate response = productService.updateProduct(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update product status", description = "Activate or deactivate a product (Admin only)")
    public ResponseEntity<ResponseErrorTemplate> updateProductStatus(
            @Parameter(description = "Product ID", example = "1") @PathVariable Long id,
            @Parameter(description = "Active status", example = "true") @RequestParam Boolean isActive) {
        ResponseErrorTemplate response = productService.updateProductStatus(id, isActive);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete product", description = "Delete a product by ID (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product ID", example = "1") @PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}