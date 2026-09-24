package com.example.learning_spring_security.dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AdminCreateUserRequest(
        String username,
        String password,
        String email,
        String phone,
        @JsonProperty("full_name")
        String fullName,
        @JsonProperty("role_ids")
        List<Long> roleIds,
        @JsonProperty("group_ids")
        List<Long> groupIds,
        @JsonProperty("permission_ids")
        List<Long> permissionIds
) {
}