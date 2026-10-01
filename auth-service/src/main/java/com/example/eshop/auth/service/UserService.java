package com.example.eshop.auth.service;

import com.example.eshop.auth.dto.request.AdminCreateUserRequest;
import com.example.eshop.auth.dto.request.UserRequest;
import com.example.eshop.auth.dto.response.ResponseErrorTemplate;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface UserService {
    ResponseErrorTemplate createUser(AdminCreateUserRequest request);
    ResponseErrorTemplate getUserById(Long id);
    ResponseErrorTemplate updateUser(Long id, UserRequest request);
    void deleteUser(Long id);
    List<ResponseErrorTemplate> getAllUsers();
    ResponseErrorTemplate changeUserStatus(Long id, String status);
    ResponseErrorTemplate updateProfilePicture(Long userId, MultipartFile profilePictureUrl);
    Long countUsers();
    List<ResponseErrorTemplate> searchUsers(String keyword);
    ResponseErrorTemplate changeUserPassword(Long userId, String oldPassword,String newPassword);
}