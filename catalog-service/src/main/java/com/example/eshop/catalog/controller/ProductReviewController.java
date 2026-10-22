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

@RestController @RequiredArgsConstructor
public class ProductReviewController {
    private final ProductReviewRepository reviews;
    private final ProductSkuRepository skus;
    private final ProductRepository products;
    private final StorefrontAccessClient access;
    private final LivePermissionService permissions;
    private final AuditLogService audit;
    @GetMapping({"/api/v1/storefront/products/{id}/reviews","/api/v1/products/{id}/reviews"})
    @Transactional(readOnly=true)
    public Object published(@PathVariable long id) {
        // Registered users can read public reviews even when anonymous browsing is disabled.
        boolean customer=false;try {CurrentActor.userId();customer=true;} catch(org.springframework.security.access.AccessDeniedException ignored) { }
        if(!customer) access.require(access.current().guestBrowsing());
        return reviews.findTop100ByProductIdAndModerationStatusOrderByCreatedAtDesc(id,"APPROVED").stream().map(this::publicView).toList();
    }
    @PostMapping("/internal/commerce/guest/reviews") @PreAuthorize("hasAuthority('SERVICE_ORDER')") @Transactional
    public Object guest(@Valid @RequestBody GuestReviewSubmission request) {
        access.require(access.current().guestReviews());
        var sku=skus.findById(request.skuId()).orElseThrow(()->new ResourceNotFoundException("Product not found"));
        if(!sku.getProduct().getId().equals(request.productId())||!Boolean.TRUE.equals(sku.getProduct().getIsActive())
                ||Boolean.TRUE.equals(sku.getProduct().getDeleted())) throw new ResourceNotFoundException("Product not found");
        var review=new ProductReview(); review.setProductId(request.productId());review.setGuestSessionId(request.guestSessionId());
        review.setAuthor(request.author().trim());review.setRating(request.rating());review.setTitle(request.title().trim());
        review.setBody(request.body().trim());review.setVerifiedPurchase(request.verifiedPurchase());reviews.saveAndFlush(review);
        return Map.of("id",review.getId(),"status","PENDING");
    }
    public record CustomerReview(@Min(1) @Max(5) int rating,@NotBlank @Size(max=120) String title,@NotBlank @Size(max=2000) String body) { }
    @PostMapping("/api/v1/products/{id}/reviews") @Transactional
    public Object customer(@PathVariable long id,@Valid @RequestBody CustomerReview request) {
        long userId=CurrentActor.userId();
        products.findByIdNotDeleted(id).filter(p->Boolean.TRUE.equals(p.getIsActive())).orElseThrow(()->new ResourceNotFoundException("Product not found"));
        var review=new ProductReview();review.setProductId(id);review.setUserId(userId);review.setAuthor("Customer");
        review.setRating(request.rating());review.setTitle(request.title().trim());review.setBody(request.body().trim());
        // Purchase verification is not inferred from sign-in. Moderator approval is required here too.
        reviews.saveAndFlush(review);return Map.of("id",review.getId(),"status","PENDING");
    }
    @GetMapping("/api/v1/admin/guest-reviews") @Transactional(readOnly=true)
    public Object pending(@RequestParam(defaultValue="0") int page) {
        permissions.require("PRODUCT_REVIEW_MODERATE");
        if(page<0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return reviews.findByModerationStatus("PENDING",PageRequest.of(page,20,Sort.by("id"))).map(this::moderationView);
    }
    public record Decision(@NotBlank @Pattern(regexp="APPROVED|REJECTED") String status) { }
    @PutMapping("/api/v1/admin/guest-reviews/{id}") @Transactional
    public Object moderate(@PathVariable long id,@Valid @RequestBody Decision body) {
        permissions.require("PRODUCT_REVIEW_MODERATE");
        var review=reviews.findById(id).orElseThrow(()->new ResourceNotFoundException("Review not found"));
        if(!"PENDING".equals(review.getModerationStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT);
        review.setModerationStatus(body.status());review.setModeratedBy(CurrentActor.userId());review.setModeratedAt(Instant.now());
        audit.record(new AuditEvent(AuditAction.UPDATE,"PRODUCT_REVIEW",Long.toString(id),null,Map.of("moderationStatus",body.status()),AuditResult.SUCCESS,null));
        return Map.of("id",id,"status",body.status());
    }
    private Map<String,Object> publicView(ProductReview r) {
        return Map.of("id",r.getId().toString(),"author",r.getAuthor(),"rating",r.getRating(),"title",r.getTitle(),"body",r.getBody(),
                "createdAt",r.getCreatedAt(),"verifiedPurchase",r.isVerifiedPurchase(),"photos",List.of(),"helpfulCount",0);
    }
    private Map<String,Object> moderationView(ProductReview r) {
        var result=new LinkedHashMap<>(publicView(r));result.put("productId",r.getProductId());
        result.put("customerType",r.getGuestSessionId()==null?"CUSTOMER":"GUEST");result.put("moderationStatus",r.getModerationStatus());return result;
    }
}
