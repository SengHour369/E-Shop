package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.UserRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface UserService {
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
