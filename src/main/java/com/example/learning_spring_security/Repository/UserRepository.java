package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findFirstByUsernameAndStatus(String username, String status);
    Optional<User> findFirstByUsernameOrEmail(String username, String email);
    Boolean existsByUsernameAndDeletedFalse(String username);
    @Query("SELECT p FROM User p WHERE LOWER(p.fullName)  LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.username) LIKE LOWER(CONCAT('%', :keyword, '%')) And (p.deleted IS NULL OR p.deleted = False)")
    List<User> searchUsers(@Param("keyword")String keyword);
    @Query("SELECT u FROM User u WHERE LOWER(u.username) = LOWER(:name) AND (u.deleted IS NULL OR u.deleted = False)")
    Optional<User> findByUsername(@Param("name") String name);
    Optional<User> findByEmail(String email);
    @Query("SELECT u FROM User u WHERE (LOWER(u.username) = LOWER(:credential) OR LOWER(u.email) = LOWER(:credential)) AND (u.deleted IS NULL OR u.deleted = False) AND u.status = :status")
    Optional<User> findByUsernameOrEmailAndStatus(@Param("credential") String credential, @Param("status") String status);
//    Optional<User> findByVerificationCode(String verificationCode);
    @Query("""
            SELECT u FROM User u
            WHERE (u.deleted IS NULL OR u.deleted = False)
    """)
    List<User> findAllUser();
    @Query("""
            SELECT u FROM User u
            WHERE u.id =:id AND (u.deleted IS NULL OR u.deleted = False)
    """)
    Optional<User> findUserById(@Param("id") Long id);

    Boolean existsByIdAndDeletedFalse(Long id);

}
