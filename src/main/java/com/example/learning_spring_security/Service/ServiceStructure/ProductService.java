package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.ProductRequest;
import com.example.learning_spring_security.dto.Response.ProductResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ProductService {
    ResponseErrorTemplate createProduct(ProductRequest request) throws Exception;
    ResponseErrorTemplate getProductById(Long id);
    ResponseErrorTemplate getProductWithSkus(Long id);
    Page<ResponseErrorTemplate> getAllProducts(Pageable pageable);
    Page<ResponseErrorTemplate> getActiveProducts(Pageable pageable);
    Page<ResponseErrorTemplate> getProductsBySubCategory(Long subCategoryId, Pageable pageable);
    Page<ResponseErrorTemplate> getProductsByCategory(Long categoryId, Pageable pageable);
    Page<ResponseErrorTemplate> searchProducts(String keyword, Pageable pageable);
    ResponseErrorTemplate updateProduct(Long id, ProductRequest request);
    void deleteProduct(Long id);
    ResponseErrorTemplate updateProductStatus(Long id, Boolean isActive);
    ResponseErrorTemplate addImageToProduct(Long productId, MultipartFile file);
}