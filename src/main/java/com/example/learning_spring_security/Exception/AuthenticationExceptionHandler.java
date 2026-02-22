package com.example.learning_spring_security.Exception;

import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthenticationExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ResponseErrorTemplate> handleBadCredentialsException(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ResponseErrorTemplate(
                        "Invalid username or password",
                        String.valueOf(HttpStatus.UNAUTHORIZED.value()),
                        null
                ));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ResponseErrorTemplate> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ResponseErrorTemplate(
                        ex.getMessage(),
                        String.valueOf(HttpStatus.UNAUTHORIZED.value()),
                        null
                ));
    }
}