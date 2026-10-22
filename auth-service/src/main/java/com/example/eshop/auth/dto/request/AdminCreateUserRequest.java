package com.example.eshop.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import jakarta.validation.constraints.*;

public record AdminCreateUserRequest(
        @NotBlank(message="Username is required") String username,
        @NotBlank(message="Password is required") String password,
        @NotBlank(message="Email is required") @Email(message="Enter a valid email address") String email,
        @NotBlank(message="Phone number is required") String phone,
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