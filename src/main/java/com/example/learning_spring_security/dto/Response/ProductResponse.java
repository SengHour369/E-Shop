package com.example.learning_spring_security.dto.Response;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    @JsonProperty("main_image")
    private List<String> Image;
    @JsonProperty("is_active")
    private Boolean isActive;
    private List<ProductSkuResponse> skus;
}