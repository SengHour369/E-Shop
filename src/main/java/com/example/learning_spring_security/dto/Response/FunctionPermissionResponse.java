package com.example.learning_spring_security.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FunctionPermissionResponse {

    @JsonProperty("func_id")
    private Long funcId;

    @JsonProperty("func_code")
    private String funcCode;

    @JsonProperty("func_name")
    private String funcName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("module")
    private String module;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}