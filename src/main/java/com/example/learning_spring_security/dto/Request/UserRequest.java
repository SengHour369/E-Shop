package com.example.learning_spring_security.dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record UserRequest(
        String username,
        String password,
        String email,

        @JsonProperty("full_name") String fullName){

}

