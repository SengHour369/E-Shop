package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.RefreshToken;
import com.example.learning_spring_security.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);
}