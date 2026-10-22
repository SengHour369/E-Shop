package com.example.eshop.order.guest;

import com.example.eshop.common.storefront.*;
import com.example.eshop.order.repository.OrderRepository;
import com.example.eshop.order.service.impl.CatalogAccess;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;

@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/guest/reviews")
public class GuestReviewController {
    private final GuestSessionService sessions;
    private final GuestRateLimiter limiter;
    private final StorefrontAccessClient access;
    private final CatalogAccess catalog;
    private final OrderRepository orders;
    public record Review(@Positive @NotNull Long productId,@Positive @NotNull Long skuId,
            @NotBlank @Size(max=80) String author,@Min(1) @Max(5) int rating,
            @NotBlank @Size(max=120) String title,@NotBlank @Size(max=2000) String body) { }
    @PostMapping @Transactional
    public Object submit(HttpServletRequest request,@Valid @RequestBody Review body) {
        access.require(access.current().guestReviews());
        var session=sessions.require(request,true);
        if(session.getContactVerifiedAt()==null||session.getVerifiedEmail()==null) throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,"Verify contact before reviewing");
        if(body.body().matches("(?is).*(https?://|www\\.|<\\s*(script|iframe)).*"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Links and embedded markup are not accepted in guest reviews");
        limiter.require("reviews",session.getId().toString(),3,86400);
        limiter.require("review-contact",session.getVerifiedEmail(),3,86400);
        boolean verified=orders.guestPurchased(session.getId(),body.skuId());
        return catalog.guestReview(new GuestReviewSubmission(session.getId(),body.productId(),body.skuId(),body.author(),body.rating(),body.title(),body.body(),verified));
    }
}
