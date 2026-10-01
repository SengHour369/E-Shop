package com.example.eshop.catalog.service.impl;


import com.example.eshop.common.exception.ResourceNotFoundException;
import com.example.eshop.catalog.model.Image;
import com.example.eshop.catalog.model.Product;

import com.example.eshop.catalog.model.SubCategory;
import com.example.eshop.catalog.repository.*;
import com.example.eshop.catalog.service.ImageService;
import com.example.eshop.catalog.service.ProductService;

import com.example.eshop.catalog.mapper.ProductMapper;

import com.example.eshop.catalog.dto.request.GetProductRequest;
import com.example.eshop.catalog.dto.request.ProductRequest;
import com.example.eshop.catalog.dto.request.ProductSkuRequest;
import com.example.eshop.catalog.dto.response.ProductPageResponse;
import com.example.eshop.catalog.dto.response.ProductResponse;
import com.example.eshop.catalog.dto.response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import com.example.eshop.common.audit.*;
import static com.example.eshop.catalog.audit.CatalogAuditSnapshots.of;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    private final AuditLogService audit;

    private final ProductRepository productRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final ImageService imageService;
    private final ProductResponseService productResponses;
    private final ProductSkuServiceImpl productSkuService;

    @Override
    @Transactional
    public ResponseErrorTemplate createProduct(ProductRequest request, List<MultipartFile> files, List<MultipartFile> skuImages) throws Exception {
        SubCategory subCategory = subCategoryRepository.findById(request.getSubCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + request.getSubCategoryId()));

        if (files == null || files.isEmpty()) {
            throw new Exception("file in image is empty");
        }
        List<Image> imageUrls = files.stream()
                .map(imageService::uploadImage)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Product product = ProductMapper.toEntity(request, subCategory);
        product.setImage(imageUrls);
        imageUrls.forEach(img -> img.setProduct(product));

        Product savedProduct = productRepository.save(product);
        audit.record(AuditEvent.success(AuditAction.PRODUCT_CREATE, "PRODUCT", savedProduct.getId(), null, of(savedProduct)));

        if (request.getSkus() != null && !request.getSkus().isEmpty()) {
            List<ProductSkuRequest> skus = request.getSkus();
            for (int i = 0; i < skus.size(); i++) {
                MultipartFile skuImage = skuImages != null && i < skuImages.size() ? skuImages.get(i) : null;
                this.productSkuService.createSku(savedProduct.getId(), skus.get(i), skuImage);
            }
        }

        log.info("Product created: id={}, name={}", savedProduct.getId(), savedProduct.getName());
        return productResponses.toResponse(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getProducts(GetProductRequest request) {
        log.debug("getProducts: criteriaType={}, criteriaValue={}, page={}, size={}",
                request.getCriteriaType(), request.getCriteriaValue(),
                request.getPage(), request.getSize());

        Pageable pageable = PageRequest.of(
                request.getPage() - 1,
                request.getSize(),
                Sort.by("id").descending()
        );

        Integer type = request.getCriteriaType();
        String value = request.getCriteriaValue();

        // ── Single-item lookups (no pagination needed) ──────────────
        if (Integer.valueOf(5).equals(type)) { // by ID — returns single product
            Product product = productRepository.findByIdNotDeleted(Long.parseLong(value))
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + value));
            return ResponseErrorTemplate.success("Product retrieved successfully",
                    productResponses.toProductResponse(product));
        }

        if (Integer.valueOf(6).equals(type)) { // by ID with SKUs
            Product product = productRepository.findByIdNotDeleted(Long.parseLong(value))
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + value));
            return productResponses.toResponse(product);
        }

        // ── Paginated lookups ────────────────────────────────────────
        Page<Product> page;
        String successMsg;

        if (type == null || type == 0 || (type != 4 && (value == null || value.isBlank()))) {
            page = productRepository.findAllNotDeleted(pageable);
            successMsg = "Retrieved all products";
        } else if (type == 1) { // by name (fuzzy)
            page = productRepository.searchProducts(value, pageable);
            successMsg = "Retrieved products by name";
        } else if (type == 2) { // by subCategoryId
            page = productRepository.findBySubCategoryId(Long.parseLong(value), pageable);
            successMsg = "Retrieved products by sub-category";
        } else if (type == 3) { // by categoryId
            page = productRepository.findByCategoryId(Long.parseLong(value), pageable);
            successMsg = "Retrieved products by category";
        } else if (type == 4) { // active only
            page = productRepository.findByIsActiveTrue(pageable);
            successMsg = "Retrieved active products";
        } else {
            page = productRepository.findAllNotDeleted(pageable);
            successMsg = "Retrieved all products";
        }

        List<ProductResponse> payload = productResponses.toProductResponses(page.getContent());

        ProductPageResponse pageResponse = ProductPageResponse.builder()
                .payload(payload)
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber() + 1)
                .pageSize(page.getSize())
                .build();

        String message = page.isEmpty() ? "No products found" : successMsg;
        return ResponseErrorTemplate.success(message, pageResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getProductById(Long id) {
        Product product = productRepository.findByIdNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return productResponses.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getProductWithSkus(Long id) {
        Product product = productRepository.findByIdNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return productResponses.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseErrorTemplate> getAllProducts(Pageable pageable) {
        return productResponses.toResponses(productRepository.findAllNotDeleted(pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseErrorTemplate> getActiveProducts(Pageable pageable) {
        return productResponses.toResponses(productRepository.findByIsActiveTrue(pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseErrorTemplate> getProductsBySubCategory(Long subCategoryId, Pageable pageable) {
        if (!subCategoryRepository.existsById(subCategoryId)) {
            throw new ResourceNotFoundException("SubCategory not found with id: " + subCategoryId);
        }
        return productResponses.toResponses(productRepository.findBySubCategoryId(subCategoryId, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseErrorTemplate> getProductsByCategory(Long categoryId, Pageable pageable) {
        return productResponses.toResponses(productRepository.findByCategoryId(categoryId, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseErrorTemplate> searchProducts(String keyword, Pageable pageable) {
        return productResponses.toResponses(productRepository.searchProducts(keyword, pageable));
    }

    @Override
    @Transactional
    public ResponseErrorTemplate updateProduct(Long id, ProductRequest request, List<MultipartFile> files, List<MultipartFile> skuImages) throws Exception {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        SubCategory subCategory = product.getSubCategory();
        if (request.getSubCategoryId() != null) {
            subCategory = subCategoryRepository.findById(request.getSubCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + request.getSubCategoryId()));
        }

        if (files != null && !files.isEmpty()) {

            List<Image> newImages = files.stream()
                    .map(imageService::uploadImage)
                    .filter(Objects::nonNull)
                    .toList();

            product.getImage().clear();

            for (Image image : newImages) {
                image.setProduct(product);
                product.getImage().add(image);
            }
        }
        var before = of(product);
        ProductMapper.updateEntity(product, request, subCategory);
        Product updatedProduct = productRepository.save(product);
        audit.record(AuditEvent.success(AuditAction.PRODUCT_UPDATE, "PRODUCT", id, before, of(updatedProduct)));

        if (request.getSkus() != null && !request.getSkus().isEmpty()) {
            List<ProductSkuRequest> skus = request.getSkus();
            for (int i = 0; i < skus.size(); i++) {
                ProductSkuRequest skuRequest = skus.get(i);
                MultipartFile skuImage = skuImages != null && i < skuImages.size() ? skuImages.get(i) : null;
                if (skuRequest.getProductSkuId() != null) {
                    productSkuService.updateSku(skuRequest.getProductSkuId(), skuRequest, skuImage);
                } else {
                    productSkuService.createSku(updatedProduct.getId(), skuRequest, skuImage);
                }
            }
        }

        log.info("Product updated: id={}, name={}", updatedProduct.getId(), updatedProduct.getName());
        return productResponses.toResponse(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findByIdNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        var before = of(product);
        product.setDeleted(true);
        productRepository.save(product);
        audit.record(AuditEvent.success(AuditAction.PRODUCT_DISABLE, "PRODUCT", id, before, of(product)));
    }

    @Override
    @Transactional
    public ResponseErrorTemplate updateProductStatus(Long id, Boolean isActive) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        var before = of(product);
        product.setIsActive(isActive);
        Product updatedProduct = productRepository.save(product);
        audit.record(AuditEvent.success(Boolean.TRUE.equals(isActive) ? AuditAction.PRODUCT_UPDATE : AuditAction.PRODUCT_DISABLE, "PRODUCT", id, before, of(updatedProduct)));
        return productResponses.toResponse(updatedProduct);
    }
}
