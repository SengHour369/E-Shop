package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.GetProductRequest;
import com.example.learning_spring_security.dto.Request.ProductRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ProductService {

    // Paginated – skip
    ResponseErrorTemplate getProducts(GetProductRequest request);

    @Cacheable(value = "products", key = "#id")
    ResponseErrorTemplate getProductById(Long id);

    @Cacheable(value = "products", key = "#id + ':withSkus'")
    ResponseErrorTemplate getProductWithSkus(Long id);

    @CacheEvict(value = "products", allEntries = true)
    ResponseErrorTemplate createProduct(ProductRequest request, List<MultipartFile> files, List<MultipartFile> skuImages) throws Exception;

    @CacheEvict(value = "products", allEntries = true)
    ResponseErrorTemplate updateProduct(Long id, ProductRequest request, List<MultipartFile> files, List<MultipartFile> skuImages) throws Exception;

    @CacheEvict(value = "products", key = "#id")
    ResponseErrorTemplate updateProductStatus(Long id, Boolean isActive);

    @CacheEvict(value = "products", allEntries = true)
    void deleteProduct(Long id);

    // Paginated – skip
    Page<ResponseErrorTemplate> getAllProducts(Pageable pageable);
    Page<ResponseErrorTemplate> getActiveProducts(Pageable pageable);
    Page<ResponseErrorTemplate> getProductsBySubCategory(Long subCategoryId, Pageable pageable);
    Page<ResponseErrorTemplate> getProductsByCategory(Long categoryId, Pageable pageable);
    Page<ResponseErrorTemplate> searchProducts(String keyword, Pageable pageable);
}