package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Model.Address;
import com.example.learning_spring_security.dto.Request.AddressRequest;
import com.example.learning_spring_security.dto.Response.AddressResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;

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