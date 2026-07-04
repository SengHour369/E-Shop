package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.GetProductRequest;
import com.example.learning_spring_security.dto.Request.ProductRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {
    ResponseErrorTemplate getProducts(GetProductRequest request);
    ResponseErrorTemplate getProductById(Long id);
    ResponseErrorTemplate getProductWithSkus(Long id);
    ResponseErrorTemplate createProduct(ProductRequest request, List<MultipartFile> files) throws Exception;
    ResponseErrorTemplate updateProduct(Long id, ProductRequest request, List<MultipartFile> files) throws Exception;
    ResponseErrorTemplate updateProductStatus(Long id, Boolean isActive);
    void deleteProduct(Long id);

    // kept for backward compat
    Page<ResponseErrorTemplate> getAllProducts(Pageable pageable);
    Page<ResponseErrorTemplate> getActiveProducts(Pageable pageable);
    Page<ResponseErrorTemplate> getProductsBySubCategory(Long subCategoryId, Pageable pageable);
    Page<ResponseErrorTemplate> getProductsByCategory(Long categoryId, Pageable pageable);
    Page<ResponseErrorTemplate> searchProducts(String keyword, Pageable pageable);
}