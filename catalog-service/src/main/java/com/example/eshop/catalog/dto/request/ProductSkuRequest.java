package com.example.eshop.catalog.dto.request;

import com.example.eshop.catalog.dto.response.ProductAttributeValueResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * ProductSkuRequest DTO
 *
 * Related classes in this package:
 * - {@link ProductAttributeRequest} - For creating new attributes inline
 * - {@link SkuAttributeAssignmentRequest} - Lightweight attribute assignment
 */
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class ProductSkuRequest {
    Long productSkuId;

    private String description;

    /** Optional retail barcode. Omitted on update leaves the stored barcode unchanged. Blank clears it. */
    @jakarta.validation.constraints.Size(max = 64, message = "Barcode is too long")
    private String barcode;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;


    @NotNull(message = "Quantity is required")
    @jakarta.validation.constraints.PositiveOrZero(message = "Quantity must be zero or positive")
    private Long quantity;

    @JsonProperty("low_stock_threshold")
    private Integer lowStockThreshold = 5;

    @JsonProperty("is_default")
    private Boolean isDefault = false;
    private Boolean OperatorProductAttribute = false;
    @Valid
    private InventoryRequest inventory;


    /**
     * Optional: Create new attributes inline during SKU creation
     * These will be created and assigned to this SKU
     *
     * @see ProductAttributeRequest
     */
    @JsonProperty("product_attributes")
    private List<ProductAttributeRequest> productAttributes =  new ArrayList<>();


}