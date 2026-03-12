package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import com.example.learning_spring_security.dto.Request.Register;

import java.util.Optional;

public interface AuthService {
     ResponseErrorTemplate create(Register userRequest);
     Optional<Long> findById(String username);
     ResponseErrorTemplate logIn(String username);
    void requestPasswordReset(String email);
    void resetPassword(String token, String newPassword);
    void changePassword(Long userId, String currentPassword, String newPassword);
}
