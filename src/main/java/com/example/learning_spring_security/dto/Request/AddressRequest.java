package com.example.learning_spring_security.dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class AddressRequest {

    @NotBlank(message = "Address line 1 is required")
    @JsonProperty("address_line1")
    private String addressLine1;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Zip code is required")
    @JsonProperty("zip_code")
    private String zipCode;

    @NotBlank(message = "Country is required")
    private String country;

    @JsonProperty("is_default")
    private Boolean isDefault = false;
}