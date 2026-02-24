package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import com.example.learning_spring_security.dto.Request.Register;

public interface AuthService {
     ResponseErrorTemplate create(Register userRequest);
     ResponseErrorTemplate findById(Long id);
     ResponseErrorTemplate logIn(String username);
}
