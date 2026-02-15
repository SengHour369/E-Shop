package com.example.learning_spring_security.dto.Request;

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
}