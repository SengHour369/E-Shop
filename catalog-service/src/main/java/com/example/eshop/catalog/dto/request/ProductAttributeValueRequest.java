package com.example.eshop.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class ProductAttributeValueRequest {

    private Long id;

    @NotBlank(message = "Attribute value is required")
    private String value;
}