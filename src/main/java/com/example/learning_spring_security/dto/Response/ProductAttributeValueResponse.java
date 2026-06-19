package com.example.learning_spring_security.dto.Response;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class ProductAttributeValueResponse {
    private Long id;
    private String value;
    private Long attributeId;
    private String attributeName;
}

