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
    //private List<String> image;

    @JsonProperty("main_image")
    private String mainImage;

    @JsonProperty("is_active")
    private Boolean isActive;

    private List<ProductSkuResponse> skus;
}