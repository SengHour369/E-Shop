package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.UserGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {
    List<UserGroup> findByUserIdAndIsDeleteFalse(Long userId);

    Page<UserGroup> findByIsDeleteFalse(Pageable pageable);
    Page<UserGroup> findByGroupIdAndIsDeleteFalse(Long groupId, Pageable pageable);
    Page<UserGroup> findByUserIdAndIsDeleteFalse(Long userId, Pageable pageable);
    Page<UserGroup> findByIsActiveAndIsDeleteFalse(Boolean isActive, Pageable pageable);
    boolean existsByUserIdAndGroupId(Long userId, Long groupId);
    List<UserGroup> findByUserId(Long userId);

}