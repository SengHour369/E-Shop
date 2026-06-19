package com.example.learning_spring_security.dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class SkuAttributeAssignmentRequest {

    @NotNull(message = "Attribute ID is required")
    @JsonProperty("attribute_id")
    private Long attributeId;

    @NotNull(message = "Attribute value ID is required")
    @JsonProperty("attribute_value_id")
    private Long attributeValueId;
}

