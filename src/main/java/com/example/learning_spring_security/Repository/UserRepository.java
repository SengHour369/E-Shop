package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.User;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findFirstByUsernameAndStatus(String username, String status);
    Optional<User> findFirstByUsernameOrEmail(String username, String email);
    Boolean existsByUsername(String username);
    @Query("SELECT p FROM User p WHERE LOWER(p.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.username) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchUsers(@Param("keyword")String keyword);
    Optional<User> findByUsername(String name);
//    Optional<User> findByVerificationCode(String verificationCode);
}
