package com.example.learning_spring_security.dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class ProductAttributeRequest {

    private Long id;

    @NotBlank(message = "Attribute name is required")
    private String name;

    /**
     * Optional: Simple attribute assignments with just IDs
     * Alternative to variantAttributes for lightweight requests
     *
     * @see SkuAttributeAssignmentRequest
     */
    @JsonProperty("attributes")
    private List<ProductAttributeValueRequest> attributes = new ArrayList<>();
}

