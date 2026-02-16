//package com.example.learning_spring_security.controller;
//
//
//import com.example.learning_spring_security.Service.AuthService;
//import io.swagger.v3.oas.annotations.security.SecurityRequirement;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.security.Principal;
//
//@RestController
//@RequestMapping("api/v1/user")
//@Slf4j
//@RequiredArgsConstructor
//@SecurityRequirement(name = "BearerAuth")
//
//public class UserController {
//
//    private final AuthService userService;
//
//
//    @GetMapping("/accounts")
//    public ResponseEntity<Object> findUserByUsername(Principal principal) {
//        var username = principal.getName();
//        log.info("User: username {} request get user data", username);
//        return ResponseEntity.ok(userService.logIn(username));
//    }
//}
