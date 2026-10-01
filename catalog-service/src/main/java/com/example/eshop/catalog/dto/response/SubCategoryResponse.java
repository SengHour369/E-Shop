package com.example.eshop.catalog.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

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
    private Boolean Status;
    private Long CategoryId;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}