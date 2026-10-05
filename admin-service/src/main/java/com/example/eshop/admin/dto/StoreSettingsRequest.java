package com.example.eshop.admin.dto;

import jakarta.validation.constraints.*;

public record StoreSettingsRequest(
        @NotBlank @Size(max = 120) String storeName,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 40) String phone,
        @Size(max = 500) String address,
        @NotBlank @Size(max = 80) String timezone) {
}
