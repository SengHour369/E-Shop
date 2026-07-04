package com.example.learning_spring_security.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionResponse {

    @JsonProperty("user_permission_id")
    private Long userPermissionId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("func_id")
    private Long funcId;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}