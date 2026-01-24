package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findFirstByUsernameAndStatus(String username, String status);
    Optional<User> findFirstByUsernameOrEmail(String username, String email);;
}
