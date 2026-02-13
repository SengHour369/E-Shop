package com.example.learning_spring_security.Service;

import com.example.learning_spring_security.dto.ResponseErrorTemplate;
import com.example.learning_spring_security.dto.UserRequest;

public interface AuthService {
     ResponseErrorTemplate create(UserRequest userRequest);
     ResponseErrorTemplate findById(Long id);
     ResponseErrorTemplate logIn(String username);
}
