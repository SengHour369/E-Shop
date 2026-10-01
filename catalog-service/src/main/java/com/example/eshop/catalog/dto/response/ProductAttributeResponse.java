package com.example.eshop.catalog.dto.response;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class ProductAttributeResponse {
    private Long id;
    private String name;
    private List<ProductAttributeValueResponse> attributes;
}

