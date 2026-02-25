package com.example.learning_spring_security.Service.ServiceImplement;

import com.example.learning_spring_security.Service.ServiceStructure.UserService;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class UserServiceImpl implements UserService {


    @Override
    public ResponseErrorTemplate getUserByEmail(String email) {
        return null;
    }

    @Override
    public ResponseErrorTemplate getUserByUsername(String username) {
        return null;
    }


    @Override
    public ResponseErrorTemplate getUserById(Long id) {
        return null;
    }

    @Override
    public List<ResponseErrorTemplate> getAllUsers() {
        return List.of();
    }


    @Override
    public ResponseErrorTemplate DeleteUserById(Long id) {
        return null;
    }

    @Override
    public ResponseErrorTemplate DeleteUserByUsername(String username) {
        return null;
    }

    @Override
    public ResponseErrorTemplate addUserImage(Long id, MultipartFile file) {
        return null;
    }
}
