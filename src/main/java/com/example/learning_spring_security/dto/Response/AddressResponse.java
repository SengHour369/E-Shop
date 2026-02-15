package com.example.learning_spring_security.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class AddressResponse {
    private Long id;

    @JsonProperty("address_line1")
    private String addressLine1;

    @JsonProperty("address_line2")
    private String addressLine2;

    private String city;
    private String state;

    @JsonProperty("zip_code")
    private String zipCode;

    private String country;

    @JsonProperty("is_default")
    private Boolean isDefault;
}