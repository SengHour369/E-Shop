package com.example.eshop.catalog.dto.response;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class CategoryResponseWithSubCategory {
        private Long id;
        private String name;
        private String description;
        private List<ResponseErrorTemplate> subCategories;
    }

