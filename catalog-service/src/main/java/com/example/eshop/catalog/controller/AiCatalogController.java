package com.example.eshop.catalog.controller;
import com.example.eshop.catalog.repository.*;
import com.example.eshop.catalog.service.impl.*;
import com.example.eshop.catalog.dto.request.*;
import com.example.eshop.catalog.enumeration.*;
import com.example.eshop.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
/** Fixed adapters reuse domain validation and audit. The caller's JWT is retained. */
@RestController @RequestMapping("/internal/ai") @RequiredArgsConstructor
public class AiCatalogController {
    private final ProductRepository products;
    private final ProductSkuRepository skus;
    private final ProductResponseService responses;
    private final PromotionService promotions;
    @GetMapping("/products/{id}") @Transactional(readOnly = true)
    public Object product(@PathVariable Long id) {
        var p = products.findById(id).filter(v -> Boolean.TRUE.equals(v.getIsActive()) && !Boolean.TRUE.equals(v.getDeleted()))
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return responses.toProductResponse(p);
    }
    @GetMapping("/skus/{id}") @Transactional(readOnly = true)
    public Object sku(@PathVariable Long id) {
        var sku = skus.findById(id).orElseThrow(() -> new ResourceNotFoundException("SKU not found"));
        var p = sku.getProduct();
        if (!Boolean.TRUE.equals(p.getIsActive()) || Boolean.TRUE.equals(p.getDeleted())) throw new ResourceNotFoundException("SKU not found");
        return responses.toProductResponse(p).getSkus().stream().filter(s -> s.getId().equals(id)).findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("SKU not found"));
    }
    public record DraftPromotion(@Positive long skuId, @NotBlank @Size(max=120) String name,
        @NotNull @DecimalMin(value="0", inclusive=false) @DecimalMax("100") @Digits(integer=3, fraction=4) BigDecimal discount,
        @NotNull LocalDateTime startAt, @NotNull LocalDateTime endAt) {}
    @PostMapping("/promotions") @PreAuthorize("hasAuthority('ADMIN')") @Transactional
    public Object create(@Valid @RequestBody DraftPromotion draft) {
        var result = promotions.create(new PromotionRequest(draft.name(), null, null, PromotionType.PRODUCT_DISCOUNT,
            DiscountType.PERCENTAGE, draft.discount(), null, null, draft.startAt(), draft.endAt(), 0, null, null, false));
        promotions.assign(result.id(), new PromotionSkuRequest(List.of(draft.skuId())));
        return result;
    }
}
