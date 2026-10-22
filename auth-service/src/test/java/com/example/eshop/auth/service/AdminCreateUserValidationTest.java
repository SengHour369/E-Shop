package com.example.eshop.auth.service;

import com.example.eshop.auth.dto.request.AdminCreateUserRequest;
import com.example.eshop.auth.exception.BadRequestException;
import com.example.eshop.auth.repository.*;
import com.example.eshop.auth.service.impl.UserServiceImpl;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCreateUserValidationTest {
    @Mock UserRepository userRepository;
    @InjectMocks UserServiceImpl service;
    AdminCreateUserRequest request(String phone) {
        return new AdminCreateUserRequest("test", "test-password", "test@example.test", phone, "Test", List.of(5L), List.of(), List.of());
    }
    @Test void missingPhoneIsRejectedBeforePersistence() {
        assertThatThrownBy(()->service.createUser(request(null))).isInstanceOf(BadRequestException.class).hasMessage("Phone number is required");
        verifyNoInteractions(userRepository);
    }
    @Test void duplicatePhoneHasAnActionableError() {
        when(userRepository.existsByPhone("+85512345678")).thenReturn(true);
        assertThatThrownBy(()->service.createUser(request(" +85512345678 "))).isInstanceOf(BadRequestException.class).hasMessageContaining("already belongs");
        verify(userRepository, never()).save(any());
    }
    @Test void requestValidationIdentifiesTheMissingPhone() {
        try(var factory=Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(request(null))).anyMatch(v->v.getPropertyPath().toString().equals("phone") && v.getMessage().equals("Phone number is required"));
        }
    }
}
