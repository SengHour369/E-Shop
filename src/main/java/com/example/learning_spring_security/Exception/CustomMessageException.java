package com.example.learning_spring_security.Exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CustomMessageException extends RuntimeException {
    @JsonProperty("message")
    private String message;
    @JsonProperty("code")
    private String code;
}
