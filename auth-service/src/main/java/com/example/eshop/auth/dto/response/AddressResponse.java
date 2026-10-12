package com.example.eshop.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class AddressResponse {
    private Long id;

    @JsonProperty("label")
    private String label;

    @JsonProperty("recipient_name")
    private String recipientName;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("district")
    private String district;

    @JsonProperty("commune")
    private String commune;

    @JsonProperty("additional_information")
    private String additionalInformation;



    @JsonProperty("address_line1")
    private String addressLine1;

    private String city;


    @JsonProperty("zip_code")
    private String zipCode;

    private String country;

    @JsonProperty("is_default")
    private Boolean isDefault;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}