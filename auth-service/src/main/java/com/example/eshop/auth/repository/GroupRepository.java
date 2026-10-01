package com.example.eshop.auth.repository;

import com.example.eshop.auth.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {
    Boolean existsByGroupCode(String groupCode);
    Optional<Group> findByGroupCode(String groupCode);
}