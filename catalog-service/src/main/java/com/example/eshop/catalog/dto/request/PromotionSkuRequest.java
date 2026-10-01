package com.example.eshop.catalog.dto.request;
import jakarta.validation.constraints.*;
import java.util.List;
public record PromotionSkuRequest(@NotEmpty @Size(max = 500) List<@NotNull @Positive Long> skuIds) {}

