package com.example.learning_spring_security.dto.Response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
@Setter
@Getter
@Builder
public class UserResponse{
    private Long id;
    private String username;
    private String email;
    private String password;
    private String fullName;
    private String birthdate;
    private int attempt;
    private String status;
    private String  image;
    private List<AddressResponse> addresses;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

}
