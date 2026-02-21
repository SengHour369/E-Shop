package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.ProductRequest;
import com.example.learning_spring_security.dto.Response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);
    ProductResponse getProductById(Long id);
    ProductResponse getProductWithSkus(Long id);
    Page<ProductResponse> getAllProducts(Pageable pageable);
    Page<ProductResponse> getActiveProducts(Pageable pageable);
    Page<ProductResponse> getProductsBySubCategory(Long subCategoryId, Pageable pageable);
    Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable);
    Page<ProductResponse> searchProducts(String keyword, Pageable pageable);
    ProductResponse updateProduct(Long id, ProductRequest request);
    void deleteProduct(Long id);
    ProductResponse updateProductStatus(Long id, Boolean isActive);
    ProductResponse addImageToProduct(Long productId, MultipartFile file);
}