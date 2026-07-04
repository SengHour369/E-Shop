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

    boolean existsByGroupIdAndFuncId(Long groupId, Long funcId);

    boolean existsByGroupIdAndFuncIdAndIsActive(Long groupId, Long funcId, Boolean isActive);

    // type 0 or null: all
    Page<GroupPermission> findAll(Pageable pageable);

    // type 1: by groupId
    Page<GroupPermission> findByGroupId(Long groupId, Pageable pageable);

    // type 2: by funcId
    Page<GroupPermission> findByFuncId(Long funcId, Pageable pageable);

    // type 3: by groupId + funcId
    @Query("SELECT p FROM GroupPermission p WHERE p.groupId = :groupId AND p.funcId = :funcId")
    Page<GroupPermission> findByGroupIdAndFuncId(@Param("groupId") Long groupId, @Param("funcId") Long funcId, Pageable pageable);

    // type 4: by isActive
    Page<GroupPermission> findByIsActive(Boolean isActive, Pageable pageable);

    // type 5: by groupId + isActive
    Page<GroupPermission> findByGroupIdAndIsActive(Long groupId, Boolean isActive, Pageable pageable);
}