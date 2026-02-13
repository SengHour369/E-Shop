package com.example.learning_spring_security;

import com.example.learning_spring_security.Model.Role;
import com.example.learning_spring_security.Repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class LearningSpringSecurityApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearningSpringSecurityApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(RoleRepository roleRepository) {
        return args -> {
            List<String> roles = List.of("ADMIN", "USER");

            for (String roleName : roles) {
                if (roleRepository.findByName(roleName).isEmpty()) {
                    Role role = new Role();
                    role.setName(roleName);
                    roleRepository.save(role);
                    System.out.println("Role created: " + roleName);
                } else {
                    System.out.println("Role already exists: " + roleName);
                }
            }
        };
    }
}
