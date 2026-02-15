package com.example.learning_spring_security.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class CategoryResponse {
    private Long id;
    private String name;
    private String description;

    @JsonProperty("sub_categories")
    private List<SubCategoryResponse> subCategories;
}