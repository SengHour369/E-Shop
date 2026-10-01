package com.example.eshop.catalog.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    private String name;
    private String description;
    private Boolean status;

    @JsonProperty("icon_id")
    private Long iconId;
}