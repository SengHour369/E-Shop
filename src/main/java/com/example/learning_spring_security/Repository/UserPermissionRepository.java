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

    // Check existence only among non‑deleted records
    @Query("SELECT COUNT(p) > 0 FROM UserPermission p WHERE p.userId = :userId AND p.funcId = :funcId AND p.isDelete = false")
    boolean existsByUserIdAndFuncId(@Param("userId") Long userId, @Param("funcId") Long funcId);

//    @Query("SELECT COUNT(p) > 0 FROM UserPermission p WHERE p.userId = :userId AND p.funcId = :funcId AND p.isActive = :isActive AND p.isDelete = false")
//    boolean existsByUserIdAndFuncIdAndIsActive(@Param("userId") Long userId, @Param("funcId") Long funcId, @Param("isActive") Boolean isActive);

    // All methods below filter by isDelete = false
    @Query("SELECT p FROM UserPermission p WHERE p.isDelete = false")
    Page<UserPermission> findAll(Pageable pageable);

    @Query("SELECT p FROM UserPermission p WHERE p.userId = :userId AND p.isDelete = false")
    Page<UserPermission> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT p FROM UserPermission p WHERE p.funcId = :funcId AND p.isDelete = false")
    Page<UserPermission> findByFuncId(@Param("funcId") Long funcId, Pageable pageable);

    @Query("SELECT p FROM UserPermission p WHERE p.userId = :userId AND p.funcId = :funcId AND p.isDelete = false")
    Page<UserPermission> findByUserIdAndFuncId(@Param("userId") Long userId, @Param("funcId") Long funcId, Pageable pageable);

    @Query("SELECT p FROM UserPermission p WHERE p.isActive = :isActive AND p.isDelete = false")
    Page<UserPermission> findByIsActive(@Param("isActive") Boolean isActive, Pageable pageable);

    @Query("SELECT p FROM UserPermission p WHERE p.userId = :userId AND p.isActive = :isActive AND p.isDelete = false")
    Page<UserPermission> findByUserIdAndIsActive(@Param("userId") Long userId, @Param("isActive") Boolean isActive, Pageable pageable);
    // សម្រាប់ពិនិត្យសិទ្ធិផ្ទាល់របស់អ្នកប្រើ
    boolean existsByUserIdAndFuncIdAndIsActive(Long userId, Long funcId, boolean isActive);

}