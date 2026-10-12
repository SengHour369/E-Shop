package com.example.eshop.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class AddressRequest {

    @Size(max = 40)
    @JsonProperty("label")
    private String label;

    @Size(max = 100)
    @JsonProperty("recipient_name")
    private String recipientName;

    @Size(max = 30)
    @JsonProperty("phone_number")
    private String phoneNumber;

    @Size(max = 100)
    @JsonProperty("district")
    private String district;

    @Size(max = 100)
    @JsonProperty("commune")
    private String commune;

    @Size(max = 500)
    @JsonProperty("additional_information")
    private String additionalInformation;



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