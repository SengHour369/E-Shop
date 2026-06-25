package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.Product;
import com.example.learning_spring_security.Model.ProductAttribute;
import com.example.learning_spring_security.Model.ProductSku;
import com.example.learning_spring_security.Repository.ProductAttributeRepository;
import com.example.learning_spring_security.Repository.ProductRepository;
import com.example.learning_spring_security.Repository.ProductSkuRepository;

import com.example.learning_spring_security.Service.ServiceStructure.ProductSkuService;
import com.example.learning_spring_security.ServiceMapper.ProductMapper;
import com.example.learning_spring_security.ServiceMapper.ProductSkuMapper;
import com.example.learning_spring_security.dto.Request.ProductAttributeRequest;

import com.example.learning_spring_security.dto.Request.ProductSkuRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSkuServiceImpl implements ProductSkuService {

    private final ProductSkuRepository productSkuRepository;
    private final ProductRepository productRepository;
    private  final ProductAttributeServiceImpl  productAttributeServiceImpl;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductSku createSku(Long productId, ProductSkuRequest request) {

        // 2. Fetch the parent product
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        // 3. Map request to entity
        ProductSku sku = ProductSkuMapper.toEntity(request, product);

        // 4. Handle default SKU logic: only one default per product
        if (Boolean.TRUE.equals(sku.getIsDefault())) {
            clearExistingDefaultSku(productId);
        }

        // 5. Save

        ProductSku saved = productSkuRepository.save(sku);

        for (ProductAttributeRequest productAttributeRequest : request.getProductAttributes()) {
            this.productAttributeServiceImpl.createAttribute(saved.getId(), productAttributeRequest);
        }
        log.info("Created SKU: {} for product ID: {}", saved.getSku(), productId);
        return saved;
    }

    @Override
    @Transactional
    public ProductSku updateSku(Long skuId, ProductSkuRequest request) {
        ProductSku existing = productSkuRepository.findById(skuId)
                .orElseThrow(() -> new ResourceNotFoundException("SKU not found with id: " + skuId));

        if (!existing.getSku().equals(request.getSku()) &&
                productSkuRepository.existsBySku(request.getSku())) {
            throw new ResourceNotFoundException("SKU already exists: " + request.getSku());
        }

        ProductSkuMapper.updateEntity(existing, request);
        ProductSku updated = productSkuRepository.save(existing);

        if (request.getProductAttributes() != null && !request.getProductAttributes().isEmpty()) {
            for (com.example.learning_spring_security.dto.Request.ProductAttributeRequest attributeRequest : request.getProductAttributes()) {
                if (attributeRequest.getId() != null) {
                    productAttributeServiceImpl.updateAttribute(attributeRequest.getId(), attributeRequest);
                } else {
                    productAttributeServiceImpl.createAttribute(updated.getId(), attributeRequest);
                }
            }
        }

        log.info("Updated SKU: {}", updated.getSku());
        return updated;
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

    @Override
    @Transactional(readOnly = true)
    public ProductSku getDefaultSkuByProductId(Long productId) {
        return productSkuRepository.findDefaultSkuByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("No default SKU found for product id: " + productId));
    }

    @Override
    @Transactional
    public void reduceStock(Long skuId, Long quantity) {
        int updated = productSkuRepository.reduceStock(skuId, quantity);
        if (updated == 0) {
            // Either SKU not found or insufficient stock
            ProductSku sku = productSkuRepository.findById(skuId)
                    .orElseThrow(() -> new ResourceNotFoundException("SKU not found with id: " + skuId));
            if (sku.getQuantity() < quantity) {
                throw new ResourceNotFoundException("Insufficient stock for SKU: " + sku.getSku() +
                        ". Available: " + sku.getQuantity() + ", requested: " + quantity);
            }
        }
        log.info("Reduced stock for SKU id {} by {}", skuId, quantity);
    }

    @Override
    @Transactional
    public void increaseStock(Long skuId, Long quantity) {
        int updated = productSkuRepository.increaseStock(skuId, quantity);
        if (updated == 0) {
            throw new ResourceNotFoundException("SKU not found with id: " + skuId);
        }
        log.info("Increased stock for SKU id {} by {}", skuId, quantity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSku> getLowStockSkus() {
        return productSkuRepository.findLowStockSkus();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSku> getLowStockSkusByProductId(Long productId) {
        return productSkuRepository.findLowStockSkusByProductId(productId);
    }

    // --------------------- Helper Methods ---------------------

    /**
     * Clears the default flag on all other SKUs of the same product.
     */
    private void clearExistingDefaultSku(Long productId) {
        productSkuRepository.findDefaultSkuByProductId(productId)
                .ifPresent(defaultSku -> {
                    defaultSku.setIsDefault(false);
                    productSkuRepository.save(defaultSku);
                });
    }
}