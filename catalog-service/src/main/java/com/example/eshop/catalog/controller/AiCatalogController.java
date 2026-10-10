package com.example.eshop.catalog.controller;

import com.example.eshop.catalog.repository.ProductRepository;
import com.example.eshop.catalog.repository.ProductSkuRepository;
import com.example.eshop.catalog.service.impl.ProductResponseService;
import com.example.eshop.catalog.service.impl.PromotionService;
import com.example.eshop.catalog.dto.request.PromotionRequest;
import com.example.eshop.catalog.dto.request.PromotionSkuRequest;
import com.example.eshop.catalog.enumeration.DiscountType;
import com.example.eshop.catalog.enumeration.PromotionType;
import com.example.eshop.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Fixed adapters reuse domain validation and audit. The caller's JWT is retained. */
@RestController
@RequestMapping("/internal/ai")
@RequiredArgsConstructor
public class AiCatalogController {

    private final ProductRepository products;
    private final ProductSkuRepository skus;
    private final ProductResponseService responses;
    private final PromotionService promotions;
    private final com.example.eshop.common.security.LivePermissionService permissions;
    private final com.example.eshop.catalog.service.InventoryService inventory;

    @GetMapping("/inventory/{skuId}")
    public Object inventory(@PathVariable long skuId) {
        permissions.require("INVENTORY_VIEW");
        return inventory.getInventoryBySkuId(skuId);
    }

    @GetMapping("/inventory/low-stock")
    public Object lowStock(@RequestParam(defaultValue = "10") long threshold) {
        permissions.require("INVENTORY_VIEW");
        if (threshold < 0 || threshold > 1_000_000) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        return inventory.getLowStockInventory(threshold, org.springframework.data.domain.PageRequest.of(0, 20));
    }

    @GetMapping("/promotions/{id}")
    public Object promotion(@PathVariable long id) {
        permissions.require("PROMOTION_VIEW");
        return promotions.get(id);
    }

    @PostMapping("/promotions/{id}/disable")
    public Object disablePromotion(@PathVariable long id) {
        permissions.require("PROMOTION_MANAGE");
        return promotions.transition(id, "disable");
    }

    @GetMapping("/products")
    @Transactional(readOnly = true)
    public Object search(@RequestParam String query) {
        if (query.isBlank() || query.length() > 120) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        var page = products.searchByNameAndActive(query, true,
                org.springframework.data.domain.PageRequest.of(0, 20));
        return responses.toProductResponses(page.getContent()).stream()
                .map(ProductCard::from)
                .toList();
    }

    @GetMapping("/products/{id}")
    @Transactional(readOnly = true)
    public Object product(@PathVariable Long id) {
        var product = products.findById(id)
                .filter(value -> Boolean.TRUE.equals(value.getIsActive()))
                .filter(value -> !Boolean.TRUE.equals(value.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return ProductCard.from(responses.toProductResponse(product));
    }

    @GetMapping("/skus/{id}")
    @Transactional(readOnly = true)
    public Object sku(@PathVariable Long id) {
        var sku = skus.findById(id).orElseThrow(() -> new ResourceNotFoundException("SKU not found"));
        var product = sku.getProduct();
        if (!Boolean.TRUE.equals(product.getIsActive()) || Boolean.TRUE.equals(product.getDeleted())) {
            throw new ResourceNotFoundException("SKU not found");
        }
        return ProductCard.from(responses.toProductResponse(product)).skus().stream()
                .filter(value -> value.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("SKU not found"));
    }

    public record DraftPromotion(
            @Positive long skuId,
            @NotBlank @Size(max = 120) String name,
            @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("100")
            @Digits(integer = 3, fraction = 4) BigDecimal discount,
            @NotNull LocalDateTime startAt,
            @NotNull LocalDateTime endAt) {
    }

    @PostMapping("/promotions")
    @Transactional
    public Object create(@Valid @RequestBody DraftPromotion draft) {
        permissions.require("PROMOTION_MANAGE");
        var request = new PromotionRequest(
                draft.name(), null, null, PromotionType.PRODUCT_DISCOUNT,
                DiscountType.PERCENTAGE, draft.discount(), null, null,
                draft.startAt(), draft.endAt(), 0, null, null, false);
        var result = promotions.create(request);
        promotions.assign(result.id(), new PromotionSkuRequest(List.of(draft.skuId())));
        return result;
    }

    public record ProductCard(Long id, String name, String description, List<String> images, List<SkuCard> skus) {

        static ProductCard from(com.example.eshop.catalog.dto.response.ProductResponse product) {
            return new ProductCard(product.getId(), product.getName(), product.getDescription(), product.getImage(),
                    product.getSkus().stream().map(SkuCard::from).toList());
        }
    }

    public record SkuCard(Long id, String sku, BigDecimal basePrice, BigDecimal effectivePrice, Long available) {

        static SkuCard from(com.example.eshop.catalog.dto.response.ProductSkuResponse sku) {
            Long available = sku.getInventory() == null ? null : sku.getInventory().getAvailableQuantity();
            return new SkuCard(sku.getId(), sku.getSku(), sku.getPrice(), sku.getFinalPrice(), available);
        }
    }
}

