package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.GroupPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupPermissionRepository extends JpaRepository<GroupPermission, Long> {

    @Query("SELECT COUNT(p) > 0 FROM GroupPermission p WHERE p.groupId = :groupId AND p.funcId = :funcId AND p.isDelete = false")
    boolean existsByGroupIdAndFuncId(@Param("groupId") Long groupId, @Param("funcId") Long funcId);

    @Query("SELECT COUNT(p) > 0 FROM GroupPermission p WHERE p.groupId = :groupId AND p.funcId = :funcId AND p.isActive = :isActive AND p.isDelete = false")
    boolean existsByGroupIdAndFuncIdAndIsActive(@Param("groupId") Long groupId, @Param("funcId") Long funcId, @Param("isActive") Boolean isActive);

    @Query("SELECT p FROM GroupPermission p WHERE p.isDelete = false")
    Page<GroupPermission> findAll(Pageable pageable);

    @Query("SELECT p FROM GroupPermission p WHERE p.groupId = :groupId AND p.isDelete = false")
    Page<GroupPermission> findByGroupId(@Param("groupId") Long groupId, Pageable pageable);

    @Query("SELECT p FROM GroupPermission p WHERE p.funcId = :funcId AND p.isDelete = false")
    Page<GroupPermission> findByFuncId(@Param("funcId") Long funcId, Pageable pageable);

    @Query("SELECT p FROM GroupPermission p WHERE p.groupId = :groupId AND p.funcId = :funcId AND p.isDelete = false")
    Page<GroupPermission> findByGroupIdAndFuncId(@Param("groupId") Long groupId, @Param("funcId") Long funcId, Pageable pageable);

    @Query("SELECT p FROM GroupPermission p WHERE p.isActive = :isActive AND p.isDelete = false")
    Page<GroupPermission> findByIsActive(@Param("isActive") Boolean isActive, Pageable pageable);

    @Query("SELECT p FROM GroupPermission p WHERE p.groupId = :groupId AND p.isActive = :isActive AND p.isDelete = false")
    Page<GroupPermission> findByGroupIdAndIsActive(@Param("groupId") Long groupId, @Param("isActive") Boolean isActive, Pageable pageable);



}