package com.example.learning_spring_security.dto.Request;

import com.example.learning_spring_security.dto.Response.ProductAttributeValueResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @NotBlank(message = "SKU is required")
    private String sku;
    
    private String description;
    
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;
    
//    private String color;
//
//    private String size;
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Long quantity;
    
    @JsonProperty("low_stock_threshold")
    private Integer lowStockThreshold = 5;
    
    @JsonProperty("is_default")
    private Boolean isDefault = false;

    /**
     * Optional: Create new attributes inline during SKU creation
     * These will be created and assigned to this SKU
     * 
     * @see ProductAttributeRequest
     */
    @JsonProperty("product_attributes")
    private List<ProductAttributeRequest> productAttributes =  new ArrayList<>();
//    /**
//     * Optional: Assign existing attributes to this SKU during creation
//     * Each item specifies which attribute and value to assign
//     *
//     * @see VariantAttributeRequest
//     */
//    @JsonProperty("variant_attributes")
//    private List<VariantAttributeRequest> variantAttributes;


}