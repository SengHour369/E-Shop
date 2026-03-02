package com.example.learning_spring_security.dto.Response;

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

