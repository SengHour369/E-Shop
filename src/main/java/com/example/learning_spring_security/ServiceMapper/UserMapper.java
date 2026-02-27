package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Constant.Constant;

import com.example.learning_spring_security.Model.User;
import com.example.learning_spring_security.dto.Request.UserRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import com.example.learning_spring_security.dto.Response.UserResponse;

import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public static User toEntity(UserRequest request) {
        return User.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .fullName(request.getFullName())
                .attempt(0)
                .build();
    }

    public static ResponseErrorTemplate toResponse(User user) {
        return new ResponseErrorTemplate(Constant.SUC_MSG, Constant.SUC_CODE,UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .status(user.getStatus())
                .image(user.getImage())
                .birthdate(user.getBirthdate())
                .createdAt(user.getCreated())
                .password(user.getPassword())
                .updatedAt(user.getUpdated())
                .deletedAt(user.getDeletedAt())
                .build());
    }

}