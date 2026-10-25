package com.example.eshop.catalog.controller;

import com.example.eshop.catalog.model.ProductReview;
import com.example.eshop.catalog.repository.*;
import com.example.eshop.common.storefront.*;
import com.example.eshop.common.security.*;
import com.example.eshop.common.audit.*;
import com.example.eshop.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.time.Instant;

@RestController
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductReviewRepository reviews;
    private final ProductSkuRepository skus;
    private final ProductRepository products;
    private final StorefrontAccessClient access;
    private final LivePermissionService permissions;
    private final AuditLogService audit;

    @GetMapping({
            "/api/v1/storefront/products/{id}/reviews",
            "/api/v1/products/{id}/reviews"
    })
    @Transactional(readOnly = true)
    public Object published(@PathVariable long id) {
        // Registered users can read public reviews even when anonymous browsing is disabled.
        boolean customer = false;

        try {
            CurrentActor.userId();
            customer = true;
        } catch (org.springframework.security.access.AccessDeniedException ignored) {
        }

        if (!customer) {
            access.require(access.current().guestBrowsing());
        }

        return reviews.findTop100ByProductIdAndModerationStatusOrderByCreatedAtDesc(
                        id,
                        "APPROVED"
                )
                .stream()
                .map(this::publicView)
                .toList();
    }

    @PostMapping("/internal/commerce/guest/reviews")
    @PreAuthorize("hasAuthority('SERVICE_ORDER')")
    @Transactional
    public Object guest(@Valid @RequestBody GuestReviewSubmission request) {
        access.require(access.current().guestReviews());
        var sku = skus.findById(request.skuId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found"
                ));

        if (!sku.getProduct().getId().equals(request.productId())
                || !Boolean.TRUE.equals(sku.getProduct().getIsActive())
                || Boolean.TRUE.equals(sku.getProduct().getDeleted())) {
            throw new ResourceNotFoundException("Product not found");
        }

        var review = new ProductReview();
        review.setProductId(request.productId());
        review.setGuestSessionId(request.guestSessionId());
        review.setAuthor(request.author().trim());
        review.setRating(request.rating());
        review.setTitle(request.title().trim());
        review.setBody(request.body().trim());
        review.setVerifiedPurchase(request.verifiedPurchase());
        reviews.saveAndFlush(review);
        return Map.of("id", review.getId(), "status", "PENDING");
    }

    public record CustomerReview(
            @Min(1) @Max(5) int rating,
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 2000) String body
    ) {}

    @PostMapping("/api/v1/products/{id}/reviews")
    @Transactional
    public Object customer(
            @PathVariable long id,
            @Valid @RequestBody CustomerReview request
    ) {
        long userId = CurrentActor.userId();
        products.findByIdNotDeleted(id)
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found"
                ));
        var review = new ProductReview();
        review.setProductId(id);
        review.setUserId(userId);
        review.setAuthor("Customer");
        review.setRating(request.rating());
        review.setTitle(request.title().trim());
        review.setBody(request.body().trim());
        // Purchase verification is not inferred from sign-in. Moderator approval is required here too.
        reviews.saveAndFlush(review);
        return Map.of("id", review.getId(), "status", "PENDING");
    }

    @GetMapping("/api/v1/admin/guest-reviews")
    @Transactional(readOnly = true)
    public Object pending(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "") String type,
            @RequestParam(required = false) java.time.LocalDate from,
            @RequestParam(required = false) java.time.LocalDate to) {
        permissions.require("PRODUCT_REVIEW_MODERATE");
        if (page < 0 || !Set.of("", "ALL", "PENDING", "APPROVED", "REJECTED").contains(status)
                || !Set.of("", "GUEST", "CUSTOMER").contains(type)
                || rating != null && (rating < 1 || rating > 5)
                || from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid review filters");
        }
        var base = reviewFilter(search, rating, type, from, to);
        var selected = base;
        if (!status.isEmpty() && !status.equals("ALL")) selected = selected.and((root,query,cb)->cb.equal(root.get("moderationStatus"),status));
        var result = reviews.findAll(selected, PageRequest.of(page,20,Sort.by(Sort.Direction.DESC,"createdAt").and(Sort.by(Sort.Direction.DESC,"id"))));
        Map<Long,String> names = new HashMap<>();
        products.findAllById(result.getContent().stream().map(ProductReview::getProductId).distinct().toList()).forEach(product->names.put(product.getId(),product.getName()));
        var content = result.map(review->{var row=moderationView(review);row.put("productName",names.getOrDefault(review.getProductId(),"Product #"+review.getProductId()));return row;}).getContent();
        Map<String,Long> counts = new LinkedHashMap<>();
        for (var state : List.of("PENDING","APPROVED","REJECTED")) counts.put(state,reviews.count(base.and((root,query,cb)->cb.equal(root.get("moderationStatus"),state))));
        counts.put("ALL",counts.values().stream().mapToLong(Long::longValue).sum());
        return Map.of("content",content,"last",result.isLast(),"totalElements",result.getTotalElements(),"totalPages",result.getTotalPages(),"counts",counts);
    }

    private org.springframework.data.jpa.domain.Specification<ProductReview> reviewFilter(String search, Integer rating, String type, java.time.LocalDate from, java.time.LocalDate to) {
        return (root,query,cb)->{
            var filters=new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (rating!=null) filters.add(cb.equal(root.get("rating"),rating));
            if (type.equals("GUEST")) filters.add(cb.isNotNull(root.get("guestSessionId")));
            if (type.equals("CUSTOMER")) filters.add(cb.isNull(root.get("guestSessionId")));
            if (from!=null) filters.add(cb.greaterThanOrEqualTo(root.get("createdAt"),from.atStartOfDay(java.time.ZoneOffset.UTC).toInstant()));
            if (to!=null) filters.add(cb.lessThan(root.get("createdAt"),to.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()));
            if (!search.isBlank()) {
                var pattern="%"+search.trim().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
                var productQuery=query.subquery(Long.class);var product=productQuery.from(com.example.eshop.catalog.model.Product.class);
                productQuery.select(product.get("id")).where(cb.like(cb.lower(product.get("name")),pattern,'!'));
                filters.add(cb.or(cb.like(cb.lower(root.get("author")),pattern,'!'),cb.like(cb.lower(root.get("title")),pattern,'!'),cb.like(cb.lower(root.get("body")),pattern,'!'),root.get("productId").in(productQuery)));
            }
            return cb.and(filters.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    public record Decision(
            @NotBlank @Pattern(regexp = "APPROVED|REJECTED") String status
    ) {}

    @PutMapping("/api/v1/admin/guest-reviews/{id}")
    @Transactional
    public Object moderate(
            @PathVariable long id,
            @Valid @RequestBody Decision body
    ) {
        permissions.require("PRODUCT_REVIEW_MODERATE");
        var review = reviews.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Review not found"
                ));

        if (!"PENDING".equals(review.getModerationStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        review.setModerationStatus(body.status());
        review.setModeratedBy(CurrentActor.userId());
        review.setModeratedAt(Instant.now());
        audit.record(
                new AuditEvent(
                        AuditAction.UPDATE,
                        "PRODUCT_REVIEW",
                        Long.toString(id),
                        null,
                        Map.of("moderationStatus", body.status()),
                        AuditResult.SUCCESS,
                        null
                )
        );
        return Map.of("id", id, "status", body.status());
    }

    private Map<String, Object> publicView(ProductReview r) {
        return Map.of(
                "id", r.getId().toString(),
                "author", r.getAuthor(),
                "rating", r.getRating(),
                "title", r.getTitle(),
                "body", r.getBody(),
                "createdAt", r.getCreatedAt(),
                "verifiedPurchase", r.isVerifiedPurchase(),
                "photos", List.of(),
                "helpfulCount", 0
        );
    }

    private Map<String, Object> moderationView(ProductReview r) {
        var result = new LinkedHashMap<>(publicView(r));
        result.put("productId", r.getProductId());
        result.put(
                "customerType",
                r.getGuestSessionId() == null ? "CUSTOMER" : "GUEST"
        );
        result.put("moderationStatus", r.getModerationStatus());
        return result;
    }
}