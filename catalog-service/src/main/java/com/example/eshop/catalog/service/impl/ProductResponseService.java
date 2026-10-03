package com.example.eshop.catalog.service.impl;

import com.example.eshop.catalog.constant.Constant;
import com.example.eshop.catalog.dto.response.*;
import com.example.eshop.catalog.mapper.ProductMapper;
import com.example.eshop.catalog.mapper.ProductDetails;
import com.example.eshop.catalog.model.*;
import com.example.eshop.catalog.repository.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

/** Loads one page's related data in batches; mapping itself performs no database calls. */
@Service
@RequiredArgsConstructor
public class ProductResponseService {
    private final ProductRepository products;
    private final ProductSkuRepository skus;
    private final ProductAttributeRepository attributes;
    private final ProductMapper mapper;
    private final PromotionPricingService pricing;


    public List<ProductResponse> toProductResponses(List<Product> page) {
        if (page.isEmpty()) return List.of();
        List<Long> productIds = page.stream().map(Product::getId).toList();
        Map<Long, List<String>> images = new java.util.HashMap<>();
        for (var image : products.findImageUrls(productIds)) {
            images.computeIfAbsent(image.getProductId(), key -> new java.util.ArrayList<>()).add(image.getUrl());
        }
        var skuRows = skus.findWithInventoryForProducts(productIds);
        List<ProductSku> skuList = skuRows.stream().map(ProductSkuRepository.SkuInventory::getSku).toList();
        List<Long> skuIds = skuList.stream().map(ProductSku::getId).toList();
        Map<Long, Inventory> inventoryBySku = new java.util.HashMap<>();
        for (var row : skuRows) {
            if (row.getInventory() != null) inventoryBySku.put(row.getSku().getId(), row.getInventory());
        }
        // Left joins retain SKUs without inventory and attributes without values.
        Map<Long, ProductAttribute> attributeById = new java.util.LinkedHashMap<>();
        Map<Long, List<ProductAttributeValue>> valuesByAttribute = new java.util.HashMap<>();
        if (!skuIds.isEmpty()) {
            for (var row : attributes.findWithValuesForSkus(skuIds)) {
                var attribute = row.getAttribute();
                attributeById.putIfAbsent(attribute.getId(), attribute);
                if (row.getAttributeValue() != null) {
                    valuesByAttribute.computeIfAbsent(attribute.getId(), key -> new java.util.ArrayList<>())
                            .add(row.getAttributeValue());
                }
            }
        }
        ProductDetails details = new ProductDetails(
                skuList.stream().collect(Collectors.groupingBy(sku -> sku.getProduct().getId())),
                attributeById.values().stream().collect(Collectors.groupingBy(ProductAttribute::getProductSkuId)),
                valuesByAttribute, inventoryBySku, pricing.calculatePrices(skuList), images);
        return page.stream().map(product -> mapper.toProductResponse(product, details)).toList();
    }

    public ProductResponse toProductResponse(Product product) {
        return toProductResponses(List.of(product)).get(0);
    }

    public ResponseErrorTemplate toResponse(Product product) {
        return wrap(toProductResponse(product));
    }

    public Page<ResponseErrorTemplate> toResponses(Page<Product> page) {
        var responses = toProductResponses(page.getContent()).iterator();
        return page.map(product -> wrap(responses.next()));
    }

    private ResponseErrorTemplate wrap(ProductResponse product) {
        return new ResponseErrorTemplate(Constant.SUC_MSG, Constant.SUC_CODE, product);
    }
}
