package com.example.eshop.catalog.service.impl;

import com.example.eshop.catalog.dto.request.*;
import com.example.eshop.catalog.dto.response.*;
import com.example.eshop.catalog.enumeration.*;
import com.example.eshop.catalog.model.*;
import com.example.eshop.catalog.repository.*;
import com.example.eshop.common.exception.*;
import lombok.RequiredArgsConstructor;
import com.example.eshop.common.audit.*;
import static com.example.eshop.catalog.audit.CatalogAuditSnapshots.of;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class PromotionService {
    private final AuditLogService audit;
    private final PromotionRepository promotions;
    private final PromotionSkuRepository assignments;
    private final ProductSkuRepository skus;
    private final ProductRepository products;
    private final ProductResponseService productResponses;
    private final Clock clock;
    private final PromotionUsageRepository usages;

    public PromotionResponse create(PromotionRequest request) {
        Promotion p = new Promotion();
        p.setCreatedBy(actor());
        apply(p, request);
        promotions.save(p);
        audit.record(AuditEvent.success(AuditAction.PROMOTION_CREATE, "PROMOTION", p.getId(), null, of(p)));
        return PromotionResponse.from(p);
    }
    public PromotionResponse update(Long id, PromotionRequest request) {
        Promotion p = locked(id);
        if (p.getStatus() != PromotionStatus.DRAFT)
            throw new BusinessLogicException("Only draft promotions can be edited; create a new promotion for new terms");
        var before = of(p);
        apply(p, request);
        audit.record(AuditEvent.success(AuditAction.PROMOTION_UPDATE, "PROMOTION", id, before, of(p)));
        return PromotionResponse.from(p);
    }
    @Transactional(readOnly = true)
    public PromotionResponse get(Long id) { return PromotionResponse.from(found(id)); }
    @Transactional(readOnly = true)
    public Page<PromotionResponse> list(PromotionStatus status, int page, int size) {
        return promotions.search(status, page(page, size)).map(PromotionResponse::from);
    }
    public void assign(Long id, PromotionSkuRequest request) {
        Promotion p = locked(id);
        if (p.getStatus() == PromotionStatus.DISABLED || p.getStatus() == PromotionStatus.EXPIRED)
            throw new BusinessLogicException("Cannot assign SKUs to an ended promotion");
        for (Long skuId : new TreeSet<>(request.skuIds())) {
            ProductSku sku = skus.findById(skuId).orElseThrow(() -> new ResourceNotFoundException("SKU not found: " + skuId));
            if (!Boolean.TRUE.equals(sku.getProduct().getIsActive()) || Boolean.TRUE.equals(sku.getProduct().getDeleted()))
                throw new BusinessLogicException("Promotion requires an active product");
            if (!assignments.existsByPromotionIdAndProductSkuId(id, skuId)) {
                PromotionSku link = new PromotionSku();
                link.setPromotion(p); link.setProductSku(sku); assignments.save(link);
                audit.record(AuditEvent.success(AuditAction.PROMOTION_SKU_ADD, "PROMOTION", id, null, Map.of("skuId", skuId)));
            }
        }
    }
    public void remove(Long id, Long skuId) {
        locked(id);
        if (assignments.existsByPromotionIdAndProductSkuId(id, skuId)) {
            assignments.deleteByPromotionIdAndProductSkuId(id, skuId);
            audit.record(AuditEvent.success(AuditAction.PROMOTION_SKU_REMOVE, "PROMOTION", id, Map.of("skuId", skuId), null));
        }
    }
    @Transactional(readOnly = true)
    public Page<Long> assigned(Long id, int page, int size) {
        found(id); return assignments.skuIds(id, page(page, size));
    }
    public PromotionResponse transition(Long id, String action) {
        Promotion p = locked(id);
        var before = of(p);
        LocalDateTime now = LocalDateTime.now(clock);
        if ("disable".equals(action)) {
            p.setStatus(PromotionStatus.DISABLED); p.setActive(false);
        } else {
            if (!p.isActive() || p.getStatus() == PromotionStatus.DISABLED || p.getStatus() == PromotionStatus.EXPIRED)
                throw new BusinessLogicException("Ended promotions cannot be restarted");
            if (!now.isBefore(p.getEndAt())) throw new BusinessLogicException("Promotion has already ended");
            if ("activate".equals(action) && now.isBefore(p.getStartAt()))
                throw new BusinessLogicException("Promotion has not started; schedule it instead");
            p.setStatus(now.isBefore(p.getStartAt()) ? PromotionStatus.SCHEDULED : PromotionStatus.ACTIVE);
        }
        p.setUpdatedBy(actor());
        AuditAction auditAction = "disable".equals(action) ? AuditAction.PROMOTION_DISABLE :
            p.getStatus() == PromotionStatus.ACTIVE ? AuditAction.PROMOTION_ACTIVATE : AuditAction.PROMOTION_SCHEDULE;
        audit.record(AuditEvent.success(auditAction, "PROMOTION", id, before, of(p)));
        return PromotionResponse.from(p);
    }
    @Transactional(readOnly = true)
    public Page<PromotionResponse> active(int page, int size) {
        return promotions.active(LocalDateTime.now(clock), page(page, size)).map(p -> {
            PromotionResponse r = PromotionResponse.from(p);
            return new PromotionResponse(r.id(), r.name(), r.code(), r.description(), r.promotionType(),
                r.discountType(), r.discountValue(), r.maxDiscountAmount(), r.minimumOrderAmount(),
                r.startAt(), r.endAt(), PromotionStatus.ACTIVE, r.priority(), null, null,
                false, true, null, null, null, null);
        });
    }
    @Transactional(readOnly = true)
    public Page<ProductResponse> products(Long id, int page, int size) {
        Promotion p = found(id);
        if (!PromotionPricingService.effective(p, LocalDateTime.now(clock))
                || (p.getUsageLimit() != null && usages.countByPromotionIdAndReleasedFalse(id) >= p.getUsageLimit())
                || Long.valueOf(0).equals(p.getUsagePerCustomer()))
            throw new ResourceNotFoundException("Active promotion not found");
        Page<Product> result = products.findPromotionProducts(id, page(page, size));
        var responses = productResponses.toProductResponses(result.getContent()).iterator();
        return result.map(product -> responses.next());
    }
    private Promotion found(Long id) {
        return promotions.findById(id).orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));
    }
    private Promotion locked(Long id) {
        return promotions.lockById(id).orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));
    }
    private void apply(Promotion p, PromotionRequest r) {
        if (r == null || r.name() == null || r.name().isBlank() || r.name().length() > 120 ||
            r.promotionType() == null || r.discountType() == null || r.startAt() == null || r.endAt() == null ||
            r.discountValue() == null || r.discountValue().signum() <= 0 ||
            (r.maxDiscountAmount() != null && r.maxDiscountAmount().signum() < 0) ||
            (r.minimumOrderAmount() != null && r.minimumOrderAmount().signum() < 0) ||
            (r.usageLimit() != null && r.usageLimit() < 0) ||
            (r.usagePerCustomer() != null && r.usagePerCustomer() < 0))
            throw new BusinessLogicException("Invalid promotion terms");
        if (!r.startAt().isBefore(r.endAt())) throw new BusinessLogicException("startAt must be before endAt");
        if (r.discountType() == DiscountType.PERCENTAGE && r.discountValue().compareTo(new java.math.BigDecimal("100")) > 0)
            throw new BusinessLogicException("Percentage cannot exceed 100");
        if (r.stackable()) throw new BusinessLogicException("Stacking is not supported; use stackable=false");
        p.setName(r.name().trim()); p.setCode(r.code() == null || r.code().isBlank() ? null : r.code().trim().toUpperCase(Locale.ROOT));
        p.setDescription(r.description()); p.setPromotionType(r.promotionType()); p.setDiscountType(r.discountType());
        p.setDiscountValue(r.discountValue()); p.setMaxDiscountAmount(r.maxDiscountAmount());
        p.setMinimumOrderAmount(r.minimumOrderAmount()); p.setStartAt(r.startAt()); p.setEndAt(r.endAt());
        p.setPriority(r.priority()); p.setUsageLimit(r.usageLimit()); p.setUsagePerCustomer(r.usagePerCustomer());
        p.setStackable(false); p.setUpdatedBy(actor());
    }
    private static PageRequest page(int page, int size) {
        if (page < 1 || size < 1 || size > 100) throw new BusinessLogicException("page >= 1 and size 1..100 required");
        return PageRequest.of(page - 1, size, Sort.by("id").descending());
    }
    private static String actor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }
}

