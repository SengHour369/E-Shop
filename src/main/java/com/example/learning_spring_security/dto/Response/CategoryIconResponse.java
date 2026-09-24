package com.example.learning_spring_security.dto.Response;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class CategoryIconResponse {
    private Long id;
    private String name;
    private String url;
}