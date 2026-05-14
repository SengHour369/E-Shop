package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Exception.CustomMessageException;
import com.example.learning_spring_security.Model.Role;
import com.example.learning_spring_security.Model.User;
import com.example.learning_spring_security.Repository.RoleRepository;
import com.example.learning_spring_security.Repository.UserRepository;
import com.example.learning_spring_security.dto.Request.Register;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private Register registerRequest;
    private User user;
    private Role role;

    @BeforeEach
    void setUp() {
        registerRequest = new Register("testuser", "password123", "Test User", "test@example.com");
        role = Role.builder().id(1L).name("USER").build();
        user = User.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .fullName("Test User")
                .email("test@example.com")
                .roles(List.of(role))
                .attempt(0)
                .status(Constant.ACT)
                .created(LocalDateTime.now())
                .build();
    }

    @Test
    void create_ShouldCreateUserSuccessfully() {
        // Given
        when(roleRepository.findAllByNameIn(anyList())).thenReturn(List.of(role));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        ResponseErrorTemplate response = authService.create(registerRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.message()).isEqualTo(Constant.SUC_MSG);
        assertThat(response.code()).isEqualTo(Constant.SUC_CODE);
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void create_ShouldThrowException_WhenPasswordIsBlank() {
        // Given
        Register invalidRequest = new Register("testuser", "", "Test User", "test@example.com");

        // When & Then
        assertThatThrownBy(() -> authService.create(invalidRequest))
                .isInstanceOf(CustomMessageException.class)
                .hasMessage("Password can't be blank or null");
    }

    @Test
    void create_ShouldThrowException_WhenUsernameOrEmailExists() {
        // Given
        when(userRepository.findFirstByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> authService.create(registerRequest))
                .isInstanceOf(CustomMessageException.class)
                .hasMessage("Username or Email already exists.");
    }

    @Test
    void findById_ShouldReturnUserId_WhenUserExists() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // When
        Optional<Long> result = authService.findById("testuser");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(1L);
    }

    @Test
    void findById_ShouldReturnEmpty_WhenUserDoesNotExist() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When
        Optional<Long> result = authService.findById("nonexistent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findUserByUsername_ShouldReturnUser_WhenExists() {
        // Given
        when(userRepository.findByUsernameOrEmailAndStatus("testuser", Constant.ACT)).thenReturn(Optional.of(user));

        // When
        Optional<User> result = authService.findUserByUsername("testuser");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    void findUserByUsernameOrEmail_ShouldReturnUser_WhenExists() {
        // Given
        when(userRepository.findByUsernameOrEmailAndStatus("test@example.com", Constant.ACT)).thenReturn(Optional.of(user));

        // When
        Optional<User> result = authService.findUserByUsernameOrEmail("test@example.com");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void logIn_ShouldReturnUserResponse_WhenUserExists() {
        // Given
        when(userRepository.findByUsernameOrEmailAndStatus("testuser", Constant.ACT)).thenReturn(Optional.of(user));

        // When
        ResponseErrorTemplate response = authService.logIn("testuser");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.message()).isEqualTo(Constant.SUC_MSG);
        assertThat(response.code()).isEqualTo(Constant.SUC_CODE);
    }

    @Test
    void logIn_ShouldReturnErrorResponse_WhenUserDoesNotExist() {
        // Given
        when(userRepository.findByUsernameOrEmailAndStatus("nonexistent", Constant.ACT)).thenReturn(Optional.empty());

        // When
        ResponseErrorTemplate response = authService.logIn("nonexistent");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.message()).contains("not found");
        assertThat(response.code()).isEqualTo(Constant.USER_NOT_FOUND_CODE);
    }

    @Test
    void requestPasswordReset_ShouldDoNothing() {
        // When
        authService.requestPasswordReset("test@example.com");

        // Then - method is empty, so no assertions needed
        verifyNoInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void resetPassword_ShouldDoNothing() {
        // When
        authService.resetPassword("token", "newPassword");

        // Then - method is empty, so no assertions needed
        verifyNoInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void changePassword_ShouldDoNothing() {
        // When
        authService.changePassword(1L, "current", "new");

        // Then - method is empty, so no assertions needed
        verifyNoInteractions(userRepository, roleRepository, passwordEncoder);
    }
}
