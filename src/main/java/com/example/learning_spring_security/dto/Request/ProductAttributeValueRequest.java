package com.example.learning_spring_security.dto.Request;

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
