//package com.example.learning_spring_security.ServiceMapper;
//
//import com.example.learning_spring_security.Model.Role;
//import com.example.learning_spring_security.Model.User;
//import com.example.learning_spring_security.dto.Request.UserRequest;
//import com.example.learning_spring_security.dto.Response.UserResponse;
//
//import org.springframework.stereotype.Component;
//import java.util.stream.Collectors;
//
//@Component
//public class UserMapper {
//
//    public User toEntity(UserRequest request) {
//        return User.builder()
//                .username(request.username())
//                .email(request.email())
//                .password(request.password()) // នឹងត្រូវ Encrypt ក្នុង Service
//                .fullName(request.fullName())
//                .status("ACTIVE")
//                .attempt(0)
//                .build();
//    }
//
//    public UserResponse toResponse(User user) {
//        return UserResponse.builder()
//                .id(user.getId())
//                .username(user.getUsername())
//                .email(user.getEmail())
//                .fullName(user.getFullName())
//                .status(user.getStatus())
//                .roles(user.getRoles().stream()
//                        .map(Role::getName)
//                        .collect(Collectors.toList()))
//                .addresses(user.getAddresses().stream()
//                        .map(address -> AddressMapper.toResponse(address))
//                        .collect(Collectors.toList()))
//                .createdAt(user.getCreatedAt())
//                .updatedAt(user.getUpdatedAt())
//                .build();
//    }
//
//}