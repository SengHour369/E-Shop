package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {
    Boolean existsByGroupCode(String groupCode);
    Optional<Group> findByGroupCode(String groupCode);
}