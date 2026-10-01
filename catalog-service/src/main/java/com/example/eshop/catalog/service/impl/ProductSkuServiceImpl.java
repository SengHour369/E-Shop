package com.example.eshop.catalog.service.impl;

import com.example.eshop.common.exception.BusinessLogicException;
import com.example.eshop.common.exception.ResourceNotFoundException;
import com.example.eshop.catalog.model.Image;
import com.example.eshop.catalog.model.Product;
import com.example.eshop.catalog.model.ProductAttribute;
import com.example.eshop.catalog.model.ProductSku;
import com.example.eshop.catalog.repository.ProductAttributeRepository;
import com.example.eshop.catalog.repository.ProductRepository;
import com.example.eshop.catalog.repository.ProductSkuRepository;

import com.example.eshop.catalog.service.ImageService;
import com.example.eshop.catalog.service.ProductSkuService;
import com.example.eshop.catalog.mapper.ProductMapper;
import com.example.eshop.catalog.mapper.ProductSkuMapper;
import com.example.eshop.catalog.dto.request.ProductAttributeRequest;

import com.example.eshop.catalog.dto.request.ProductSkuRequest;
import com.example.eshop.catalog.util.SkuGeneratorUtil;

import lombok.RequiredArgsConstructor;
import com.example.eshop.common.audit.*;
import static com.example.eshop.catalog.audit.CatalogAuditSnapshots.of;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSkuServiceImpl implements ProductSkuService {
    private final AuditLogService audit;

    private final ProductSkuRepository productSkuRepository;
    private final ProductRepository productRepository;
    private final ProductAttributeServiceImpl productAttributeServiceImpl;
    private final InventoryServiceImpl inventoryServiceImpl;
    private final com.example.eshop.catalog.repository.InventoryRepository inventoryRepository;
    private final SkuGeneratorUtil skuGeneratorUtil;
    private final ImageService imageService;

    @Override
    @Transactional
    public ProductSku createSku(Long productId, ProductSkuRequest request, MultipartFile image) {

        // 2. Fetch the parent product
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        // 3. Map request to entity
        ProductSku sku = ProductSkuMapper.toEntity(request, product);

        // 4. Generate SKU if not provided and ensure uniqueness

        String base = skuGeneratorUtil.generateSku(product, request);

        sku.setSku(base);

        if (image != null && !image.isEmpty()) {
            sku.setImage(uploadSkuImage(image));
        }
        // 5. Save

        ProductSku saved = productSkuRepository.save(sku);
        audit.record(AuditEvent.success(AuditAction.SKU_CREATE, "PRODUCT_SKU", saved.getId(), null, of(saved)));
        if (request.getOperatorProductAttribute() != null && request.getOperatorProductAttribute()) {
            applyLowStockThreshold(request);
            inventoryServiceImpl.createInventory(saved.getId(), request.getInventory());

            for (ProductAttributeRequest productAttributeRequest : request.getProductAttributes()) {
                this.productAttributeServiceImpl.createAttribute(saved.getId(), productAttributeRequest);
            }
        }
        log.info("Created SKU: {} for product ID: {}", saved.getSku(), productId);
        return saved;
    }

    @Override
    @Transactional
    public ProductSku updateSku(Long skuId, ProductSkuRequest request, MultipartFile image) {
        ProductSku existing = productSkuRepository.findById(skuId)
                .orElseThrow(() -> new ResourceNotFoundException("SKU not found with id: " + skuId));

        Product product = productRepository.findById(existing.getProduct().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + existing.getProduct().getId()));


        var before = of(existing);
        ProductSkuMapper.updateEntity(existing, request);
        String base = skuGeneratorUtil.generateSku(product, request);

        existing.setSku(base);

        if (image != null && !image.isEmpty()) {
            existing.setImage(uploadSkuImage(image));
        }

        ProductSku updated = productSkuRepository.save(existing);
        audit.record(AuditEvent.success(AuditAction.SKU_UPDATE, "PRODUCT_SKU", skuId, before, of(updated)));

        if (request.getProductAttributes() != null && !request.getProductAttributes().isEmpty()) {
            for (com.example.eshop.catalog.dto.request.ProductAttributeRequest attributeRequest : request.getProductAttributes()) {
                if (attributeRequest.getId() != null) {
                    productAttributeServiceImpl.updateAttribute(attributeRequest.getId(), attributeRequest);
                } else {
                    productAttributeServiceImpl.createAttribute(updated.getId(), attributeRequest);
                }
            }
        }

        // Handle inventory update/create when inventory info is provided in the SKU request
        if (request.getInventory() != null) {
            applyLowStockThreshold(request);
            java.util.Optional<com.example.eshop.catalog.model.Inventory> maybeInv =
                    inventoryRepository.findByProductSkuId(updated.getId());
            if (maybeInv.isPresent()) {
                com.example.eshop.catalog.model.Inventory inv = maybeInv.get();
                inventoryServiceImpl.adjustQuantity(inv.getId(), request.getInventory());
            } else {
                inventoryServiceImpl.createInventory(updated.getId(), request.getInventory());
            }
        }

        log.info("Updated SKU: {}", updated.getSku());
        return updated;
    }

    // ProductSkuRequest carries its own top-level lowStockThreshold alongside the nested
    // InventoryRequest; propagate it so it isn't silently dropped when the nested value is unset.
    private void applyLowStockThreshold(ProductSkuRequest request) {
        if (request.getInventory() != null
                && request.getInventory().getLowStockThreshold() == null
                && request.getLowStockThreshold() != null) {
            request.getInventory().setLowStockThreshold(request.getLowStockThreshold());
        }
    }

    @Override
    @Transactional
    public void deleteSku(Long skuId) {
        if (!productSkuRepository.existsById(skuId)) {
            throw new ResourceNotFoundException("SKU not found with id: " + skuId);
        }
        productSkuRepository.deleteById(skuId);
        log.info("Deleted SKU with id: {}", skuId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductSku getSkuById(Long skuId) {
        return productSkuRepository.findById(skuId)
                .orElseThrow(() -> new ResourceNotFoundException("SKU not found with id: " + skuId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSku> getSkusByProductId(Long productId) {
        return productSkuRepository.findByProductId(productId);
    }

    private Image uploadSkuImage(MultipartFile file) {
        Image uploaded = imageService.uploadImage(file);
        if (uploaded == null) {
            throw new BusinessLogicException("Failed to upload SKU image");
        }
        return uploaded;
    }

    /**
     * Generate a base SKU using the product name and provided attribute values.
     * Now delegated to {@link SkuGeneratorUtil} for dynamic and extensible generation.
     *
     * Example: Product name "iPhone 15", Color "Blue", Storage "128GB" -> IPH15-BLU-128
     *
     * @deprecated Use {@link SkuGeneratorUtil#generateSku(Product, ProductSkuRequest)} instead
     */
    @Deprecated
    private String generateSku(Product product, ProductSkuRequest request) {
        return skuGeneratorUtil.generateSku(product, request);
    }




}