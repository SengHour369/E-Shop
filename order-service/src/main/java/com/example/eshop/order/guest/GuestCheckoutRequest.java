package com.example.eshop.order.guest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record GuestCheckoutRequest(@NotNull @Valid GuestDelivery delivery,
        @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{16,100}") String idempotencyKey,
        @NotBlank @Pattern(regexp="CASH_ON_DELIVERY|BAKONG|KHQR|ABA") String paymentMethod,
        @NotNull @DecimalMin("0.01") BigDecimal expectedTotal) { }
