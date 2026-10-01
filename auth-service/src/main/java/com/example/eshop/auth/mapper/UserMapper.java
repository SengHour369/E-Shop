package com.example.eshop.auth.mapper;

import com.example.eshop.auth.constant.Constant;

import com.example.eshop.auth.model.User;
import com.example.eshop.auth.dto.request.UserRequest;
import com.example.eshop.auth.dto.response.ResponseErrorTemplate;
import com.example.eshop.auth.dto.response.UserResponse;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UserMapper {

    public static User toEntity(UserRequest request) {
        User user = User.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .fullName(request.getFullName())
                .attempt(0)
                .deleted(false)
                .build();

        user.setCreatedAt(LocalDateTime.now());
        return  user;
    }

    public static ResponseErrorTemplate toResponse(User user) {
        return new ResponseErrorTemplate(Constant.SUC_MSG, Constant.SUC_CODE,UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .status(user.getStatus())
                // TODO: cross-service call via Feign - profile image lives in catalog-service; populate via Feign lookup if needed
                .birthdate(user.getBirthdate())
                .createdAt(user.getCreatedAt())
                .password(user.getPassword())
                .updatedAt(user.getUpdatedAt())
                .deletedAt(user.getDeletedAt())
                .build());
    }

}