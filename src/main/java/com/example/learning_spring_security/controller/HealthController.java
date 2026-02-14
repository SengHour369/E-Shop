package com.example.learning_spring_security.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "Application is running!";
    }

    @GetMapping("/")
    public String home() {
        return "Welcome to Learning Spring Security";
    }
}