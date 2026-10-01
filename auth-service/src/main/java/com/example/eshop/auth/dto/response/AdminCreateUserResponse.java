package com.example.eshop.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record AdminCreateUserResponse(
        Long id,
        String username,
        String email,
        String phone,
        @JsonProperty("full_name") String fullName,
        List<String> roles,
        @JsonProperty("group_ids") List<Long> groupIds,
        @JsonProperty("permission_ids") List<Long> permissionIds,
        @JsonProperty("created") LocalDateTime created
) {
}