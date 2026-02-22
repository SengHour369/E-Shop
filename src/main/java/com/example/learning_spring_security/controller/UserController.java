package com.example.learning_spring_security.controller;


import com.example.learning_spring_security.Service.ServiceStructure.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/user")
@Slf4j
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")

public class UserController {

    private final AuthService userService;


}
