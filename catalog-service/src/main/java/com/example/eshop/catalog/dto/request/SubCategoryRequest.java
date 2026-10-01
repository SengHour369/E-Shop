package com.example.eshop.catalog.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class SubCategoryRequest {

    @NotBlank(message = "Subcategory name is required")
    private String name;
    private String description;
    private Boolean status;
    @NotNull(message = "Category ID is required")
    @JsonProperty("category_id")
    private Long categoryId;
}