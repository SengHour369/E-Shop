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
    private final com.example.eshop.catalog.service.impl.AiProductSearchService productSearch;
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
    public Object search(@RequestParam String query,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock) {
        return productSearch.search(query, minPrice, maxPrice, inStock);
    }

    @GetMapping("/products/{id}")
    @Transactional(readOnly = true)
    public Object product(@PathVariable Long id) {
        var product = products.findById(id)
                .filter(value -> Boolean.TRUE.equals(value.getIsActive()))
                .filter(value -> !Boolean.TRUE.equals(value.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return ProductCard.from(responses.toProductResponse(product),responses.aiCategoryNames(List.of(product.getId())).get(product.getId()));
    }

    @GetMapping("/skus/{id}")
    @Transactional(readOnly = true)
    public Object sku(@PathVariable Long id) {
        var sku = skus.findById(id).orElseThrow(() -> new ResourceNotFoundException("SKU not found"));
        var product = sku.getProduct();
        if (!Boolean.TRUE.equals(product.getIsActive()) || Boolean.TRUE.equals(product.getDeleted())) {
            throw new ResourceNotFoundException("SKU not found");
        }
        return ProductCard.from(responses.toProductResponse(product),responses.aiCategoryNames(List.of(product.getId())).get(product.getId())).skus().stream()
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

    public record ProductCard(Long id, String name, String description, List<String> images, List<SkuCard> skus,
                              String productCode, String brand, String category, String currency) {
        public ProductCard(Long id,String name,String description,List<String> images,List<SkuCard> skus){this(id,name,description,images,skus,null,null,null,"USD");}
        public static ProductCard from(com.example.eshop.catalog.dto.response.ProductResponse product){return from(product,null);}
        public static ProductCard from(com.example.eshop.catalog.dto.response.ProductResponse product,String category) {
            var cards=product.getSkus().stream().map(SkuCard::from).toList();
            var brands=new java.util.LinkedHashSet<String>();
            for(var sku:cards)if(sku.attributes()!=null)for(var attribute:sku.attributes())
                if(attribute.getName()!=null&&java.util.Set.of("brand","ម៉ាក").contains(attribute.getName().toLowerCase(java.util.Locale.ROOT))&&attribute.getAttributes()!=null)
                    for(var value:attribute.getAttributes())if(value.getValue()!=null)brands.add(value.getValue());
            // Catalog has no product-level code or brand relation. Never derive either from product names.
            // USD is the existing AiShoppingService checkout currency, not a model-selected currency.
            return new ProductCard(product.getId(),product.getName(),product.getDescription(),product.getImage(),cards,
                    null,brands.size()==1?brands.iterator().next():null,category,"USD");
        }
        public ProductCard withSkus(List<SkuCard> matching){return new ProductCard(id,name,description,images,matching,productCode,brand,category,currency);}
    }

    public record SkuCard(Long id, String sku, BigDecimal basePrice, BigDecimal effectivePrice, Long available,
            Boolean isDefault, boolean hasPromotion, BigDecimal discountAmount, BigDecimal discountPercentage,
            String promotionName, List<com.example.eshop.catalog.dto.response.ProductAttributeResponse> attributes,
            String description,String imageUrl,LocalDateTime promotionStartAt,LocalDateTime promotionEndAt) {
        public SkuCard(Long id,String sku,BigDecimal basePrice,BigDecimal effectivePrice,Long available,Boolean isDefault,
                boolean hasPromotion,BigDecimal discountAmount,BigDecimal discountPercentage,String promotionName,
                List<com.example.eshop.catalog.dto.response.ProductAttributeResponse> attributes){
            this(id,sku,basePrice,effectivePrice,available,isDefault,hasPromotion,discountAmount,discountPercentage,promotionName,attributes,null,null,null,null);
        }
        static SkuCard from(com.example.eshop.catalog.dto.response.ProductSkuResponse sku) {
            Long available=sku.getInventory()==null?null:sku.getInventory().getAvailableQuantity();
            var promotion=sku.getPromotion();
            return new SkuCard(sku.getId(),sku.getSku(),sku.getPrice(),sku.getFinalPrice(),available,
                    sku.getIsDefault(),sku.isHasPromotion(),sku.getDiscountAmount(),sku.getDiscountPercentage(),
                    promotion==null?null:promotion.promotionName(),sku.getProductAttributeResponse(),sku.getDescription(),sku.getImageUrl(),
                    promotion==null?null:promotion.promotionStartAt(),promotion==null?null:promotion.promotionEndAt());
        }
    }
}
