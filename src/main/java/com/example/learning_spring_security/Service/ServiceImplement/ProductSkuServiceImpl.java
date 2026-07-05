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
import com.example.learning_spring_security.utils.SkuGeneratorUtil;

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
    private final ProductAttributeServiceImpl productAttributeServiceImpl;
    private final InventoryServiceImpl inventoryServiceImpl;
    private final com.example.learning_spring_security.Repository.InventoryRepository inventoryRepository;
    private final SkuGeneratorUtil skuGeneratorUtil;

    @Override
    @Transactional
    public ProductSku createSku(Long productId, ProductSkuRequest request) {

        // 2. Fetch the parent product
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        // 3. Map request to entity
        ProductSku sku = ProductSkuMapper.toEntity(request, product);

        // 4. Generate SKU if not provided and ensure uniqueness

            String base = skuGeneratorUtil.generateSku(product, request);

           sku.setSku(base);
        // 5. Save

        ProductSku saved = productSkuRepository.save(sku);
        inventoryServiceImpl.createInventory(saved.getId(), request.getInventory());

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

        Product product = productRepository.findById(existing.getProduct().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + existing.getProduct().getId()));


        ProductSkuMapper.updateEntity(existing, request);
        String base = skuGeneratorUtil.generateSku(product, request);

        existing.setSku(base);
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

        // Handle inventory update/create when inventory info is provided in the SKU request
        if (request.getInventory() != null) {
            java.util.Optional<com.example.learning_spring_security.Model.Inventory> maybeInv =
                    inventoryRepository.findByProductSkuId(updated.getId());
            if (maybeInv.isPresent()) {
                com.example.learning_spring_security.Model.Inventory inv = maybeInv.get();
                inventoryServiceImpl.adjustQuantity(inv.getId(), request.getInventory());
            } else {
                inventoryServiceImpl.createInventory(updated.getId(), request.getInventory());
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