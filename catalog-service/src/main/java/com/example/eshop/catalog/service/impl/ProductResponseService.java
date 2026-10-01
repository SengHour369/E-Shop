package com.example.eshop.catalog.service.impl;

import com.example.eshop.catalog.constant.Constant;
import com.example.eshop.catalog.dto.response.*;
import com.example.eshop.catalog.mapper.ProductMapper;
import com.example.eshop.catalog.mapper.ProductDetails;
import com.example.eshop.catalog.model.*;
import com.example.eshop.catalog.repository.*;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
    private final ProductAttributeValueRepository values;
    private final InventoryRepository inventories;
    private final ProductMapper mapper;
    private final PromotionPricingService pricing;


    public List<ProductResponse> toProductResponses(List<Product> page) {
        if (page.isEmpty()) return List.of();
        List<Long> productIds = page.stream().map(Product::getId).toList();
        products.fetchImages(productIds);
        List<ProductSku> skuList = skus.findForProducts(productIds);
        List<Long> skuIds = skuList.stream().map(ProductSku::getId).toList();
        List<ProductAttribute> attributeList = skuIds.isEmpty() ? List.of()
                : attributes.findByProductSkuIdInOrderById(skuIds);
        List<Long> attributeIds = attributeList.stream().map(ProductAttribute::getId).toList();
        List<ProductAttributeValue> valueList = attributeIds.isEmpty() ? List.of()
                : values.findByAttributeIdInOrderByValueAsc(attributeIds);
        List<Inventory> inventoryList = skuIds.isEmpty() ? List.of() : inventories.findByProductSkuIdIn(skuIds);
        ProductDetails details = new ProductDetails(
                skuList.stream().collect(Collectors.groupingBy(sku -> sku.getProduct().getId())),
                attributeList.stream().collect(Collectors.groupingBy(ProductAttribute::getProductSkuId)),
                valueList.stream().collect(Collectors.groupingBy(ProductAttributeValue::getAttributeId)),
                inventoryList.stream().collect(Collectors.toMap(inventory -> inventory.getProductSku().getId(), Function.identity())),
                pricing.calculatePrices(skuList));
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
