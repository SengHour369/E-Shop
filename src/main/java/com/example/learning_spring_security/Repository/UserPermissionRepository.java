package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.UserPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {

    boolean existsByUserIdAndFuncId(Long userId, Long funcId);

    boolean existsByUserIdAndFuncIdAndIsActive(Long userId, Long funcId, Boolean isActive);

    // type 0 or null: all permissions
    Page<UserPermission> findAll(Pageable pageable);

    // type 1: filter by userId
    Page<UserPermission> findByUserId(Long userId, Pageable pageable);

    // type 2: filter by funcId
    Page<UserPermission> findByFuncId(Long funcId, Pageable pageable);

    // type 3: filter by userId and funcId
    @Query("SELECT p FROM UserPermission p WHERE p.userId = :userId AND p.funcId = :funcId")
    Page<UserPermission> findByUserIdAndFuncId(@Param("userId") Long userId, @Param("funcId") Long funcId, Pageable pageable);

    // type 4: filter by isActive
    Page<UserPermission> findByIsActive(Boolean isActive, Pageable pageable);

    // type 5: filter by userId and isActive
    Page<UserPermission> findByUserIdAndIsActive(Long userId, Boolean isActive, Pageable pageable);
}