package com.example.eshop.auth.dto.request;

import com.example.eshop.auth.dto.response.AddressResponse;
import lombok.*;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.List;
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserRequest{

    private String email;
    private String password;
    private String fullName;
    private String phoneNumber;
    private String birthdate;

}