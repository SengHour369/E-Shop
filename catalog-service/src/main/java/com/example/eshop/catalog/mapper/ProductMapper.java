package com.example.eshop.catalog.mapper;


import com.example.eshop.catalog.model.*;
import com.example.eshop.catalog.dto.request.ProductRequest;

import com.example.eshop.catalog.dto.response.*;

import org.springframework.stereotype.Component;


import java.util.List;
import java.util.stream.Collectors;
@Component

public class ProductMapper {

    public static Product toEntity(ProductRequest request, SubCategory subCategory) {

        Product  product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isActive(request.getIsActive())
                .subCategory(subCategory)
                .build();

        return product;
    }


    public ProductResponse toProductResponse(Product product, ProductDetails details) {
        List<ProductSkuResponse> skuResponses =
                details.skus().getOrDefault(product.getId(), List.of())
                        .stream()
                        .map(sku -> toSkuResponse(sku, details))
                        .toList();

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .skus(skuResponses)
                .SubcategoryId(product.getSubCategory() != null ? product.getSubCategory().getId() : null)
                .Image(details.images().getOrDefault(product.getId(), List.of()))
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private ProductSkuResponse toSkuResponse(ProductSku sku, ProductDetails details) {

        List<ProductAttributeResponse> attributeResponses =
                details.attributes().getOrDefault(sku.getId(), List.of())
                        .stream()
                        .map(attribute -> toAttributeResponse(attribute, details))
                        .toList();

        // Fetch inventory (if any)
        Inventory inventory = details.inventories().get(sku.getId());

        // Map inventory entity to InventoryResponse DTO
        InventoryResponse inventoryResponse = null;
        if (inventory != null) {
            inventoryResponse = InventoryResponse.builder()
                    .id(inventory.getId())
                    .productSkuId(sku.getId())
                    .sku(sku.getSku())
                    .productId(sku.getProduct().getId())
                    .productName(sku.getProduct().getName())
                    .description(sku.getDescription()) // or inventory.getDescription() if present
                    .quantity(inventory.getQuantity())
                    .reservedQuantity(inventory.getReservedQuantity())
                    .availableQuantity(inventory.getAvailableQuantity())
                    .warehouseLocation(inventory.getWarehouseLocation())
                    .lowStockThreshold(inventory.getLowStockThreshold())
                    .stockStatus(Boolean.TRUE.equals(inventory.getIsDefault()) ? "Default" : "Non-default") // Example logic for stock status
                    .lastRestockedAt(inventory.getLastRestockedAt())
                    .createdAt(inventory.getCreatedAt())
                    .updatedAt(inventory.getUpdatedAt())
                    .build();
        }

        return ProductSkuResponse.builder()
                .id(sku.getId())
                .sku(sku.getSku())
                .barcode(sku.getBarcode())
                .description(sku.getDescription())
                .price(sku.getPrice())
                .originalPrice(details.prices().get(sku.getId()).originalPrice())
                .finalPrice(details.prices().get(sku.getId()).finalPrice())
                .discountAmount(details.prices().get(sku.getId()).discountAmount())
                .discountPercentage(details.prices().get(sku.getId()).discountPercentage())
                .hasPromotion(details.prices().get(sku.getId()).promotionApplied())
                .promotion(details.prices().get(sku.getId()))
                .isDefault(sku.getIsDefault())
                .OperatorProductAttribute(sku.getOperatorProductAttribute())
                .quantity(inventory != null ? inventory.getQuantity() : null) // kept for backward compatibility
                .imageUrl(sku.getImage() != null ? sku.getImage().getUrl() : null)
                .ProductAttributeResponse(attributeResponses)
                .inventory(inventoryResponse) // ✅ full inventory details
                .build();
    }

    private ProductAttributeResponse toAttributeResponse(ProductAttribute attribute, ProductDetails details) {

        List<ProductAttributeValueResponse> valueResponses =
                details.values().getOrDefault(attribute.getId(), List.of())
                        .stream()
                        .map(v -> ProductAttributeValueResponse.builder()
                                .id(v.getId())
                                .value(v.getValue())
                                .build())
                        .toList();

        return ProductAttributeResponse.builder()
                .id(attribute.getId())
                .name(attribute.getName())
                .attributes(valueResponses)
                .build();
    }

    public static void updateEntity(Product product,
                                    ProductRequest request,
                                    SubCategory subCategory) {

        product.setName(request.getName());

        product.setDescription(request.getDescription());
        product.setIsActive(request.getIsActive());
        product.setSubCategory(subCategory);
    }
}
