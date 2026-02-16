package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Model.Address;
import com.example.learning_spring_security.dto.Request.AddressRequest;
import com.example.learning_spring_security.dto.Response.AddressResponse;

public class AddressMapper {

    public static Address toEntity(AddressRequest request) {
        return Address.builder()
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .zipCode(request.getZipCode())
                .country(request.getCountry())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .build();
    }

    public static AddressResponse toResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .country(address.getCountry())
                .isDefault(address.getIsDefault())
                .build();
    }

    public static void updateEntity(Address address, AddressRequest request) {
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setZipCode(request.getZipCode());
        address.setCountry(request.getCountry());
        address.setIsDefault(request.getIsDefault());
    }
}