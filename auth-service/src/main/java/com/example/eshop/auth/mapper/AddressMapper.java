package com.example.eshop.auth.mapper;

import com.example.eshop.auth.constant.Constant;
import com.example.eshop.auth.model.Address;
import com.example.eshop.auth.dto.request.AddressRequest;
import com.example.eshop.auth.dto.response.AddressResponse;
import com.example.eshop.auth.dto.response.ResponseErrorTemplate;

public class AddressMapper {

    public static Address toEntity(AddressRequest request) {
        return Address.builder()
                .addressLine1(request.getAddressLine1())
                .city(request.getCity())
                .zipCode(request.getZipCode())
                .country(request.getCountry())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .build();
    }

    public static ResponseErrorTemplate toResponse(Address address) {
        AddressResponse addressResponse  = AddressResponse.builder()
                .id(address.getId())
                .addressLine1(address.getAddressLine1())
                .city(address.getCity())
                .zipCode(address.getZipCode())
                .country(address.getCountry())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
        return  new  ResponseErrorTemplate(Constant.SUC_MSG, Constant.SUC_CODE, addressResponse);
    }

    public static void updateEntity(Address address, AddressRequest request) {
        address.setAddressLine1(request.getAddressLine1());
        address.setCity(request.getCity());
        address.setZipCode(request.getZipCode());
        address.setCountry(request.getCountry());
        address.setIsDefault(request.getIsDefault());
    }
}