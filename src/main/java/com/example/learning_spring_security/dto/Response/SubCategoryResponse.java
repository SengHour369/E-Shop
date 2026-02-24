package com.example.learning_spring_security.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class SubCategoryResponse {
    private Long id;
    private String name;
    private String description;
    private String image;
    @JsonProperty("category_name")
    private String categoryName;
}