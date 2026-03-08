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

    private String city;


    @JsonProperty("zip_code")
    private String zipCode;

    private String country;

    @JsonProperty("is_default")
    private Boolean isDefault;
}