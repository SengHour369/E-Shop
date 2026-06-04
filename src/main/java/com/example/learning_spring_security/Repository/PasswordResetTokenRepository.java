package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.PasswordResetToken;
import com.example.learning_spring_security.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByToken(String token);

    void deleteByUser(User user);
}