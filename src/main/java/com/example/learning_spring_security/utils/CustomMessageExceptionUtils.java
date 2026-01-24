package com.example.learning_spring_security.utils;


import com.example.learning_spring_security.Exception.CustomMessageException;
import org.springframework.http.HttpStatus;

public class CustomMessageExceptionUtils {

    private CustomMessageExceptionUtils() {}

    public static CustomMessageException unauthorized() {
        CustomMessageException messageException = new CustomMessageException();
        messageException.setMessage("Unauthorized");
        messageException.setCode(String.valueOf(HttpStatus.UNAUTHORIZED.value()));
        return messageException;
    }

}
