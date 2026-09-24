package com.example.learning_spring_security;

import com.example.learning_spring_security.Model.Role;
import com.example.learning_spring_security.Repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

@SpringBootApplication
@EnableRetry
public class LearningSpringSecurityApplication {

    public static void main(String[] args) throws IOException {
        SpringApplication.run(LearningSpringSecurityApplication.class, args);
    }
}
