package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.AddressRequest;
import com.example.learning_spring_security.dto.Response.AddressResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AddressService {
    ResponseErrorTemplate createAddress(AddressRequest request, Long userId);
    ResponseErrorTemplate getAddressById(Long id);
    Page<ResponseErrorTemplate> getUserAddresses(Long userId, Pageable pageable);
    List<ResponseErrorTemplate> getUserAddresses(Long userId);
    ResponseErrorTemplate updateAddress(Long id, AddressRequest request, Long userId);
    void deleteAddress(Long id, Long userId);
    ResponseErrorTemplate setDefaultAddress(Long addressId, Long userId);
    ResponseErrorTemplate getDefaultAddress(Long userId);
}