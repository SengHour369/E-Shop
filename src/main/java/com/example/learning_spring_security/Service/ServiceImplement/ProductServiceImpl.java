package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.Product;
import com.example.learning_spring_security.Model.ProductSku;
import com.example.learning_spring_security.Model.SubCategory;
import com.example.learning_spring_security.Repository.ProductRepository;
import com.example.learning_spring_security.Repository.ProductSkuRepository;
import com.example.learning_spring_security.Repository.SubCategoryRepository;
import com.example.learning_spring_security.Service.ServiceStructure.ImageService;
import com.example.learning_spring_security.Service.ServiceStructure.ProductService;
import com.example.learning_spring_security.ServiceMapper.ProductMapper;
import com.example.learning_spring_security.ServiceMapper.ProductSkuMapper;
import com.example.learning_spring_security.dto.Request.ProductRequest;
import com.example.learning_spring_security.dto.Response.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductSkuRepository productSkuRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final ImageService imageService;

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        SubCategory subCategory = subCategoryRepository.findById(request.getSubCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + request.getSubCategoryId()));

        Product product = ProductMapper.toEntity(request, subCategory);

        Product savedProduct = productRepository.save(product);

        if (request.getSkus() != null && !request.getSkus().isEmpty()) {
            List<ProductSku> skus = request.getSkus().stream()
                    .map(skuRequest -> {
                        ProductSku sku = ProductSkuMapper.toEntity(skuRequest, savedProduct);
                        sku.setProduct(savedProduct);
                        return sku;
                    })
                    .collect(Collectors.toList());
            productSkuRepository.saveAll(skus);
            savedProduct.setProductSkus(skus);
        }

        return ProductMapper.toResponse(savedProduct);
    }


    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return ProductMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductWithSkus(Long id) {
        Product product = productRepository.findByIdWithSkus(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return ProductMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(ProductMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getActiveProducts(Pageable pageable) {
        return productRepository.findByIsActiveTrue(pageable)
                .map(ProductMapper::toResponse);
    }
    @Override
    public ProductResponse addImageToProduct(Long productId, MultipartFile file) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (file != null ) {
            String imageUrl = imageService.uploadImage(file);
            product.setMainImage(imageUrl);
            productRepository.save(product);
        }

        return ProductMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsBySubCategory(Long subCategoryId, Pageable pageable) {
        if (!subCategoryRepository.existsById(subCategoryId)) {
            throw new ResourceNotFoundException("SubCategory not found with id: " + subCategoryId);
        }
        return productRepository.findBySubCategoryId(subCategoryId, pageable)
                .map(ProductMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryId(categoryId, pageable)
                .map(ProductMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        return productRepository.searchProducts(keyword, pageable)
                .map(ProductMapper::toResponse);
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        SubCategory subCategory = null;
        if (request.getSubCategoryId() != null) {
            subCategory = subCategoryRepository.findById(request.getSubCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + request.getSubCategoryId()));
        }

        ProductMapper.updateEntity(product, request, subCategory);
        Product updatedProduct = productRepository.save(product);
        return ProductMapper.toResponse(updatedProduct);
    }

    @Override
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    @Override
    public ProductResponse updateProductStatus(Long id, Boolean isActive) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setIsActive(isActive);
        Product updatedProduct = productRepository.save(product);
        return ProductMapper.toResponse(updatedProduct);
    }
}