package com.example.eshop.common.storefront;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record GuestReviewSubmission(@NotNull UUID guestSessionId,@NotNull @Positive Long productId,
        @NotNull @Positive Long skuId,@NotBlank @Size(max=80) String author,@Min(1) @Max(5) int rating,
        @NotBlank @Size(max=120) String title,@NotBlank @Size(max=2000) String body,boolean verifiedPurchase) { }
