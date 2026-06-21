package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.Image;
import com.example.learning_spring_security.Model.Product;
import com.example.learning_spring_security.Model.ProductSku;
import com.example.learning_spring_security.Model.SubCategory;
import com.example.learning_spring_security.Repository.*;
import com.example.learning_spring_security.Service.ServiceStructure.ImageService;
import com.example.learning_spring_security.Service.ServiceStructure.ProductService;
import com.example.learning_spring_security.Service.ServiceStructure.ProductSkuService;
import com.example.learning_spring_security.ServiceMapper.ProductMapper;
import com.example.learning_spring_security.ServiceMapper.ProductSkuMapper;
import com.example.learning_spring_security.dto.Request.ProductRequest;
import com.example.learning_spring_security.dto.Request.ProductSkuRequest;
import com.example.learning_spring_security.dto.Response.ProductResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final ImageService imageService;
    private final ProductMapper productMapper;
    private final ProductSkuServiceImpl productSkuService;

    @Override
    @Transactional
    public ResponseErrorTemplate createProduct(ProductRequest request, List<MultipartFile> files) throws Exception {

        SubCategory subCategory = subCategoryRepository.findById(request.getSubCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + request.getSubCategoryId()));
        List<Image> imageUrls = List.of();
        if (files == null || files.isEmpty()) {
            throw new Exception("file in image is empty");
        }
            imageUrls = files.stream()
                    .map(imageService::uploadImage)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        Product product = ProductMapper.toEntity(request, subCategory);
        product.setImage(imageUrls);
        if (imageUrls != null) {
            imageUrls.forEach(img -> img.setProduct(product));
        }
        Product savedProduct = productRepository.save(product);
        if (request.getSkus() != null && !request.getSkus().isEmpty()) {
            for(ProductSkuRequest productSkuRequest : request.getSkus()) {
                this.productSkuService.createSku(savedProduct.getId(), productSkuRequest);

            }
        }

        log.info("Product created: id={}, name={}, skus={}", savedProduct.getId(), savedProduct.getName());

        return productMapper.toResponse(savedProduct);
    }


    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getProductWithSkus(Long id) {
        Product product = productRepository.findByIdWithSkus(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseErrorTemplate> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseErrorTemplate> getActiveProducts(Pageable pageable) {
        return productRepository.findByIsActiveTrue(pageable)
                .map(productMapper::toResponse);
    }
    @Override
    @Transactional(readOnly = true)
    public Page<ResponseErrorTemplate> getProductsBySubCategory(Long subCategoryId, Pageable pageable) {
        if (!subCategoryRepository.existsById(subCategoryId)) {
            throw new ResourceNotFoundException("SubCategory not found with id: " + subCategoryId);
        }
        return productRepository.findBySubCategoryId(subCategoryId, pageable)
                .map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseErrorTemplate> getProductsByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryId(categoryId, pageable)
                .map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseErrorTemplate> searchProducts(String keyword, Pageable pageable) {
        return productRepository.searchProducts(keyword, pageable)
                .map(productMapper::toResponse);
    }

    @Override
    @Transactional
    public ResponseErrorTemplate updateProduct(Long id, ProductRequest request, List<MultipartFile> files) throws Exception {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        SubCategory subCategory = product.getSubCategory();
        if (request.getSubCategoryId() != null) {
            subCategory = subCategoryRepository.findById(request.getSubCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + request.getSubCategoryId()));
        }

        if (files != null && !files.isEmpty()) {
            List<Image> newImageUrls = files.stream()
                    .map(imageService::uploadImage)
                    .filter(url -> url != null)
                    .collect(Collectors.toList());
            product.setImage(newImageUrls);
            if (newImageUrls != null) {
                newImageUrls.forEach(img -> img.setProduct(product));
            }
        }

        ProductMapper.updateEntity(product, request, subCategory);
        Product updatedProduct = productRepository.save(product);
        log.info("Product updated: id={}, name={}", updatedProduct.getId(), updatedProduct.getName());
        return productMapper.toResponse(updatedProduct);
    }


    @Override
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    @Override
    public ResponseErrorTemplate updateProductStatus(Long id, Boolean isActive) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setIsActive(isActive);
        Product updatedProduct = productRepository.save(product);
        return productMapper.toResponse(updatedProduct);
    }


}