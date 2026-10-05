package com.example.eshop.catalog.dto.response;

    import com.fasterxml.jackson.annotation.JsonProperty;
    import lombok.*;
    import java.math.BigDecimal;
    import java.util.List;

    @AllArgsConstructor
    @NoArgsConstructor
    @Setter
    @Getter
    @Builder
    public class ProductSkuResponse {
        private Long id;
        private String sku;
        private String barcode;
        private String description;
        private BigDecimal price;
        private BigDecimal originalPrice;
        private BigDecimal finalPrice;
        private BigDecimal discountAmount;
        private BigDecimal discountPercentage;
        private boolean hasPromotion;
        private PriceResult promotion;
        private Long quantity;
        @com.fasterxml.jackson.annotation.JsonProperty("is_default")
        private Boolean isDefault;
        private Boolean OperatorProductAttribute = false;

        @JsonProperty("image_url")
        private String imageUrl;

        private InventoryResponse inventory;

        // Optional: attributes assigned to this SKU
        @JsonProperty("attributes")
        private List<ProductAttributeResponse> ProductAttributeResponse;
    }
