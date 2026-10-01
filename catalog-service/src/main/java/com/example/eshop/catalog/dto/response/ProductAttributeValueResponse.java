package com.example.eshop.catalog.dto.response;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class ProductAttributeValueResponse {
    private Long id;
    private String value;

}

