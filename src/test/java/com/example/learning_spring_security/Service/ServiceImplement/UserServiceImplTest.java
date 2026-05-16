package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Exception.ExceptionService.ResourceNotFoundException;
import com.example.learning_spring_security.Model.User;
import com.example.learning_spring_security.Repository.UserRepository;
import com.example.learning_spring_security.Service.ServiceStructure.ImageService;
import com.example.learning_spring_security.dto.Request.UserRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserRequest userRequest;
    private MultipartFile file;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .fullName("Test User")
                .email("test@example.com")
                .birthdate(String.valueOf(LocalDate.of(1990, 1, 1)))
                .status("ACTIVE")
                .build();
        userRequest = UserRequest.builder()
                .password("newPassword")
                .fullName("Updated User")
                .email("updated@example.com")
                .birthdate(String.valueOf(LocalDate.of(1990, 1, 1)))
                .build();
        file = mock(MultipartFile.class);
    }

    @Test
    void getUserById_ShouldReturnUser_WhenExists() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // When
        ResponseErrorTemplate response = userService.getUserById(1L);

        // Then
        assertThat(response).isNotNull();
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserById_ShouldThrowException_WhenNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User is not found by id");
    }

    @Test
    void updateUser_ShouldUpdateSuccessfully() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        ResponseErrorTemplate response = userService.updateUser(1L, userRequest);

        // Then
        assertThat(response).isNotNull();
        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(any(User.class));
    }


    @Test
    void deleteUser_ShouldDeleteSuccessfully() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // When
        userService.deleteUser(1L);

        // Then
        verify(userRepository).deleteById(1L);
    }


    @Test
    void getAllUsers_ShouldReturnList() {
        // Given
        when(userRepository.findAll()).thenReturn(List.of(user));

        // When
        List<ResponseErrorTemplate> response = userService.getAllUsers();

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        verify(userRepository).findAll();
    }

    @Test
    void changeUserStatus_ShouldUpdateStatusSuccessfully() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        ResponseErrorTemplate response = userService.changeUserStatus(1L, "INACTIVE");

        // Then
        assertThat(response).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void updateProfilePicture_ShouldUpdateSuccessfully() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(imageService.uploadImage(file)).thenReturn("new-image.jpg");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        ResponseErrorTemplate response = userService.updateProfilePicture(1L, file);

        // Then
        assertThat(response).isNotNull();
        verify(imageService).uploadImage(file);
        verify(userRepository).save(user);
    }



    @Test
    void countUsers_ShouldReturnCount() {
        // Given
        when(userRepository.count()).thenReturn(5L);

        // When
        Long count = userService.countUsers();

        // Then
        assertThat(count).isEqualTo(5L);
        verify(userRepository).count();
    }

    @Test
    void searchUsers_ShouldReturnList() {
        // Given
        when(userRepository.searchUsers("test")).thenReturn(List.of(user));

        // When
        List<ResponseErrorTemplate> response = userService.searchUsers("test");

        // Then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        verify(userRepository).searchUsers("test");
    }

    @Test
    void changeUserPassword_ShouldChangeSuccessfully_WhenOldPasswordMatches() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        ResponseErrorTemplate response = userService.changeUserPassword(1L, "oldPassword", "newPassword");

        // Then
        assertThat(response).isNotNull();
        verify(passwordEncoder).matches("oldPassword", "encodedPassword");
        verify(userRepository).save(user);
    }

    @Test
    void changeUserPassword_ShouldThrowException_WhenOldPasswordDoesNotMatch() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.changeUserPassword(1L, "wrongPassword", "newPassword"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Old Password Doesn't Match");
    }


}
