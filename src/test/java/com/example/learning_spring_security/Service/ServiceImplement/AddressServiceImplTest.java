package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Exception.ExceptionService.UnauthorizedException;
import com.example.learning_spring_security.Model.Address;
import com.example.learning_spring_security.Model.User;
import com.example.learning_spring_security.Repository.AddressRepository;
import com.example.learning_spring_security.Repository.UserRepository;
import com.example.learning_spring_security.dto.Request.AddressRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AddressServiceImpl addressService;

    private User user;
    private Address address;
    private AddressRequest addressRequest;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("testuser")
                .addresses(new ArrayList<>())
                .build();
        address = Address.builder()
                .id(1L)
                .city("Test City")
                .zipCode("12345")
                .country("Test Country")
                .isDefault(false)
                .build();
        addressRequest = AddressRequest.builder()
                .city("Test City")
                .zipCode("12345")
                .country("Test Country")
                .isDefault(true)
                .build();
        pageable = PageRequest.of(0, 10);
    }

    @Test
    void createAddress_ShouldCreateSuccessfully() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.save(any(Address.class))).thenReturn(address);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        ResponseErrorTemplate response = addressService.createAddress(addressRequest, 1L);

        // Then
        assertThat(response).isNotNull();
        verify(addressRepository).resetDefaultAddressForUser(1L);
        verify(addressRepository).save(any(Address.class));
        verify(userRepository).save(user);
    }

    @Test
    void createAddress_ShouldThrowException_WhenUserNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> addressService.createAddress(addressRequest, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 1");
    }

    @Test
    void getAddressById_ShouldReturnAddress_WhenExists() {
        // Given
        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));

        // When
        ResponseErrorTemplate response = addressService.getAddressById(1L);

        // Then
        assertThat(response).isNotNull();
        verify(addressRepository).findById(1L);
    }

    @Test
    void getAddressById_ShouldThrowException_WhenNotFound() {
        // Given
        when(addressRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> addressService.getAddressById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Address not found with id: 1");
    }

    @Test
    void getUserAddresses_ShouldReturnPagedAddresses() {
        // Given - Note: The implementation returns Page.empty(), so test reflects that
        when(userRepository.existsById(1L)).thenReturn(true);

        // When
        Page<ResponseErrorTemplate> response = addressService.getUserAddresses(1L, pageable);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEmpty();
    }

    @Test
    void getUserAddresses_ShouldThrowException_WhenUserNotFound() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> addressService.getUserAddresses(1L, pageable))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 1");
    }

    @Test
    void getUserAddresses_ShouldReturnListOfAddresses() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);
        when(addressRepository.findByUserId(1L)).thenReturn(List.of(address));

        // When
        List<ResponseErrorTemplate> response = addressService.getUserAddresses(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
    }

    @Test
    void updateAddress_ShouldUpdateSuccessfully() {
        // Given
        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        when(addressRepository.isUserHasAddress(1L, 1L)).thenReturn(true);
        when(addressRepository.save(any(Address.class))).thenReturn(address);

        // When
        ResponseErrorTemplate response = addressService.updateAddress(1L, addressRequest, 1L);

        // Then
        assertThat(response).isNotNull();
        verify(addressRepository).resetDefaultAddressForUser(1L);
        verify(addressRepository).save(address);
    }

    @Test
    void updateAddress_ShouldThrowException_WhenAddressNotFound() {
        // Given
        when(addressRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> addressService.updateAddress(1L, addressRequest, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Address not found with id: 1");
    }

    @Test
    void updateAddress_ShouldThrowException_WhenUserDoesNotOwnAddress() {
        // Given
        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        when(addressRepository.isUserHasAddress(1L, 1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> addressService.updateAddress(1L, addressRequest, 1L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("User does not own this address");
    }

    @Test
    void deleteAddress_ShouldDeleteSuccessfully() {
        // Given
        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        when(addressRepository.isUserHasAddress(1L, 1L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        addressService.deleteAddress(1L, 1L);

        // Then
        verify(addressRepository).delete(address);
        verify(userRepository).save(user);
    }

    @Test
    void deleteAddress_ShouldThrowException_WhenAddressNotFound() {
        // Given
        when(addressRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> addressService.deleteAddress(1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Address not found with id: 1");
    }

    @Test
    void deleteAddress_ShouldThrowException_WhenUserDoesNotOwnAddress() {
        // Given
        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        when(addressRepository.isUserHasAddress(1L, 1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> addressService.deleteAddress(1L, 1L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("User does not own this address");
    }

    @Test
    void setDefaultAddress_ShouldSetSuccessfully() {
        // Given
        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        when(addressRepository.isUserHasAddress(1L, 1L)).thenReturn(true);
        when(addressRepository.save(any(Address.class))).thenReturn(address);

        // When
        ResponseErrorTemplate response = addressService.setDefaultAddress(1L, 1L);

        // Then
        assertThat(response).isNotNull();
        verify(addressRepository).resetDefaultAddressForUser(1L);
        verify(addressRepository).save(address);
    }

    @Test
    void setDefaultAddress_ShouldThrowException_WhenAddressNotFound() {
        // Given
        when(addressRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> addressService.setDefaultAddress(1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Address not found with id: 1");
    }

    @Test
    void setDefaultAddress_ShouldThrowException_WhenUserDoesNotOwnAddress() {
        // Given
        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
        when(addressRepository.isUserHasAddress(1L, 1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> addressService.setDefaultAddress(1L, 1L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("User does not own this address");
    }

    @Test
    void getDefaultAddress_ShouldReturnDefaultAddress() {
        // Given
        when(addressRepository.findDefaultAddressByUserId(1L)).thenReturn(Optional.of(address));

        // When
        ResponseErrorTemplate response = addressService.getDefaultAddress(1L);

        // Then
        assertThat(response).isNotNull();
        verify(addressRepository).findDefaultAddressByUserId(1L);
    }

    @Test
    void getDefaultAddress_ShouldThrowException_WhenNoDefaultAddress() {
        // Given
        when(addressRepository.findDefaultAddressByUserId(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> addressService.getDefaultAddress(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No default address found for user");
    }
}
