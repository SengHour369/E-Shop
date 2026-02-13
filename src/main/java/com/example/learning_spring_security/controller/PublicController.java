package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.Service.AuthService;
import com.example.learning_spring_security.dto.AuthenticationRequest;
import com.example.learning_spring_security.dto.AuthenticationResponse;
import com.example.learning_spring_security.dto.UserRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.token.TokenService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicController {

    private final AuthService userService;


    @PostMapping("/accounts/register")
    public ResponseEntity<Object> register(@RequestBody UserRequest userRequest) {
        log.info("Intercept registration new user with req: {}", userRequest);
        return ResponseEntity.ok(userService.create(userRequest));
    }


}