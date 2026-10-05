package com.example.eshop.catalog.dto.response;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class BrandLogoResponse {
    private Long id;
    private String name;
    private String url;
}
