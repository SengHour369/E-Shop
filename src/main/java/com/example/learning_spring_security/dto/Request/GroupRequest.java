package com.example.learning_spring_security.dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupRequest {


    @NotBlank(message = "Group name cannot be blank")
    @JsonProperty("group_name")
    private String name;

    private String description;

    private String status;

    private String type;

    @JsonProperty("is_active")
    private Boolean isActive = false;
}