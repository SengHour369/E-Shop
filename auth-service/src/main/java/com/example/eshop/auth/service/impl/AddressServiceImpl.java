package com.example.eshop.auth.service.impl;

import com.example.eshop.auth.exception.ResourceNotFoundException;
import com.example.eshop.auth.exception.UnauthorizedException;
import com.example.eshop.auth.model.Address;
import com.example.eshop.auth.model.User;
import com.example.eshop.auth.repository.AddressRepository;
import com.example.eshop.auth.repository.UserRepository;
import com.example.eshop.auth.service.AddressService;
import com.example.eshop.auth.mapper.AddressMapper;
import com.example.eshop.auth.dto.request.AddressRequest;
import com.example.eshop.auth.dto.response.AddressResponse;

import com.example.eshop.auth.dto.response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    public ResponseErrorTemplate createAddress(AddressRequest request, Long userId) {
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Address address = AddressMapper.toEntity(request);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.resetDefaultAddressForUser(userId);
        }

        Address savedAddress = addressRepository.save(address);

        user.getAddresses().add(savedAddress);
        userRepository.save(user);

        return AddressMapper.toResponse(savedAddress);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getAddressById(Long id) {
        Address address = addressRepository.findByAddressId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + id));
        return AddressMapper.toResponse(address);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseErrorTemplate> getUserAddresses(Long userId, Pageable pageable) {

        if (!userRepository.existsByIdAndDeletedFalse(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        List<Address> addresses = addressRepository.findByUserId(userId);
        return Page.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseErrorTemplate> getUserAddresses(Long userId) {
        if (!userRepository.existsByIdAndDeletedFalse(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        return addressRepository.findByUserId(userId).stream()
                .map(AddressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ResponseErrorTemplate updateAddress(Long id, AddressRequest request, Long userId) {
        Optional<Address> address = addressRepository.findByAddressId(id);
           if (address.isEmpty() ) {
              log.info("address not found with id: " + id);
              throw new ResourceNotFoundException("Address not found with id: " + id);
                    }

            if (!addressRepository.isUserHasAddress(userId, id)) {
                throw new UnauthorizedException("User does not own this address");
            }


            if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.get().getIsDefault())) {
                addressRepository.resetDefaultAddressForUser(userId);
            }

            AddressMapper.updateEntity(address.get(), request);
            Address updatedAddress = addressRepository.save(address.get());
            return AddressMapper.toResponse(updatedAddress);
        }

    @Override
    public void deleteAddress(Long id, Long userId) {
        Optional<Address> address = addressRepository.findByAddressId(id);
        if(address.isEmpty() ) {
            log.info("address not found with id: " + id);
            throw new ResourceNotFoundException("Address not found with id: " + id);

        }
        if (!addressRepository.isUserHasAddress(userId, id)) {
            throw new UnauthorizedException("User does not own this address");
        }


        User user = userRepository.findUserById(userId).get();
        user.getAddresses().remove(address);
        userRepository.save(user);
        address.get().setDeleted(true);
        addressRepository.save(address.get());
    }

    @Override
    public ResponseErrorTemplate setDefaultAddress(Long addressId, Long userId) {
        Optional<Address> address = addressRepository.findByAddressId(addressId);

        if (address.isEmpty() ) {
            log.info("address not found with id: " + addressId);
            throw new ResourceNotFoundException("Address not found with id: " + addressId);

        }
        if (!addressRepository.isUserHasAddress(userId, addressId)) {
            throw new UnauthorizedException("User does not own this address");
        }

        addressRepository.resetDefaultAddressForUser(userId);
        address.get().setIsDefault(true);
        Address updatedAddress = addressRepository.save(address.get());
        return AddressMapper.toResponse(updatedAddress);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getDefaultAddress(Long userId) {
        Optional<Address> address = addressRepository.findDefaultAddressByUserId(userId);
        if (address.isEmpty()) {
            throw new ResourceNotFoundException("No default address found for user");
        }
            return AddressMapper.toResponse(address.get());

    }
}