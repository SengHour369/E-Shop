package com.example.learning_spring_security.dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @JsonProperty("is_active")
    private Boolean isActive = true;

    @NotNull(message = "Subcategory ID is required")
    @JsonProperty("sub_category_id")
    private Long subCategoryId;

    private List<ProductSkuRequest> skus;
}