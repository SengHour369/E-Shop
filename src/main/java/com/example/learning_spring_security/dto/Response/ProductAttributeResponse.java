package com.example.learning_spring_security.dto.Response;

import com.example.learning_spring_security.dto.Request.SkuAttributeAssignmentRequest;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class ProductAttributeResponse {
    private Long id;
    private String name;
    private List<ProductAttributeValueResponse> attributes;
}

