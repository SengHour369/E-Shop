package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.AddressRequest;
import com.example.learning_spring_security.dto.Response.AddressResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AddressService {
    AddressResponse createAddress(AddressRequest request, Long userId);
    AddressResponse getAddressById(Long id);
    Page<AddressResponse> getUserAddresses(Long userId, Pageable pageable);
    List<AddressResponse> getUserAddresses(Long userId);
    AddressResponse updateAddress(Long id, AddressRequest request, Long userId);
    void deleteAddress(Long id, Long userId);
    AddressResponse setDefaultAddress(Long addressId, Long userId);
    AddressResponse getDefaultAddress(Long userId);
}