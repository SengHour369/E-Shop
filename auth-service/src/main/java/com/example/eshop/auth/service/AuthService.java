package com.example.eshop.auth.service;

import com.example.eshop.auth.dto.response.ResponseErrorTemplate;
import com.example.eshop.auth.dto.request.Register;

import java.util.Optional;

public interface AuthService {
    ResponseErrorTemplate create(Register userRequest);
    Optional<Long> findById(String username);
//    void requestPasswordReset(String email);
//    void resetPassword(String token, String newPassword);
//    void changePassword(Long userId, String currentPassword, String newPassword);
}