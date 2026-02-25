package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    ResponseErrorTemplate getUserByEmail(String email);
    ResponseErrorTemplate getUserByUsername(String username);
    ResponseErrorTemplate getUserById(Long id);
    List<ResponseErrorTemplate> getAllUsers();
    ResponseErrorTemplate DeleteUserById(Long id);
    ResponseErrorTemplate DeleteUserByUsername(String username);
    ResponseErrorTemplate addUserImage(Long id, MultipartFile file);
}
