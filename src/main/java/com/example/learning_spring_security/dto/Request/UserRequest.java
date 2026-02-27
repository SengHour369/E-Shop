package com.example.learning_spring_security.dto.Request;

import com.example.learning_spring_security.dto.Response.AddressResponse;
import lombok.*;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.List;
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserRequest{

    private String email;
    private String password;
    private String fullName;
    private String phoneNumber;
    private String birthdate;

}
