package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Exception.ExceptionService.UnauthorizedException;
import com.example.learning_spring_security.Model.Address;
import com.example.learning_spring_security.Model.User;
import com.example.learning_spring_security.Repository.AddressRepository;
import com.example.learning_spring_security.Repository.UserRepository;
import com.example.learning_spring_security.Service.ServiceStructure.AddressService;
import com.example.learning_spring_security.ServiceMapper.AddressMapper;
import com.example.learning_spring_security.dto.Request.AddressRequest;
import com.example.learning_spring_security.dto.Response.AddressResponse;

import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    public ResponseErrorTemplate createAddress(AddressRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Address address = AddressMapper.toEntity(request);

        // If this is the first address or set as default, reset other defaults
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.resetDefaultAddressForUser(userId);
        }

        Address savedAddress = addressRepository.save(address);

        // Add address to user
        user.getAddresses().add(savedAddress);
        userRepository.save(user);

        return AddressMapper.toResponse(savedAddress);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getAddressById(Long id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + id));
        return AddressMapper.toResponse(address);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseErrorTemplate> getUserAddresses(Long userId, Pageable pageable) {
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        // Since we need pagination, we need to customize this query
        // For now, we'll get all and paginate manually (not efficient for large data)
        List<Address> addresses = addressRepository.findByUserId(userId);
        // Implement pagination manually or create custom query in repository
        return Page.empty(); // Placeholder - need to implement properly
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseErrorTemplate> getUserAddresses(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        return addressRepository.findByUserId(userId).stream()
                .map(AddressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ResponseErrorTemplate updateAddress(Long id, AddressRequest request, Long userId) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + id));

        // Verify user owns this address
        if (!addressRepository.isUserHasAddress(userId, id)) {
            throw new UnauthorizedException("User does not own this address");
        }

        // If setting as default, reset other defaults
        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepository.resetDefaultAddressForUser(userId);
        }

        AddressMapper.updateEntity(address, request);
        Address updatedAddress = addressRepository.save(address);
        return AddressMapper.toResponse(updatedAddress);
    }

    @Override
    public void deleteAddress(Long id, Long userId) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + id));

        // Verify user owns this address
        if (!addressRepository.isUserHasAddress(userId, id)) {
            throw new UnauthorizedException("User does not own this address");
        }

        // Remove from user's addresses
        User user = userRepository.findById(userId).get();
        user.getAddresses().remove(address);
        userRepository.save(user);

        addressRepository.delete(address);
    }

    @Override
    public ResponseErrorTemplate setDefaultAddress(Long addressId, Long userId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

        // Verify user owns this address
        if (!addressRepository.isUserHasAddress(userId, addressId)) {
            throw new UnauthorizedException("User does not own this address");
        }

        addressRepository.resetDefaultAddressForUser(userId);
        address.setIsDefault(true);
        Address updatedAddress = addressRepository.save(address);
        return AddressMapper.toResponse(updatedAddress);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseErrorTemplate getDefaultAddress(Long userId) {
        Address address = addressRepository.findDefaultAddressByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No default address found for user"));
        return AddressMapper.toResponse(address);
    }
}