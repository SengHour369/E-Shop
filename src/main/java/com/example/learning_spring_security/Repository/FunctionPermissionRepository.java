package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.FunctionPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FunctionPermissionRepository extends JpaRepository<FunctionPermission, Long> {

    Optional<FunctionPermission> findByFuncCode(String funcCode);

    boolean existsByFuncCode(String funcCode);

    Page<FunctionPermission> findByIsActive(Boolean isActive, Pageable pageable);

    Page<FunctionPermission> findByModule(String module, Pageable pageable);

    @Query("SELECT f FROM FunctionPermission f WHERE LOWER(f.funcName) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<FunctionPermission> findByFuncNameContaining(@Param("name") String name, Pageable pageable);

    @Query("SELECT f FROM FunctionPermission f WHERE LOWER(f.module) = LOWER(:module) AND f.isActive = :isActive")
    Page<FunctionPermission> findByModuleAndIsActive(@Param("module") String module,
                                                     @Param("isActive") Boolean isActive,
                                                     Pageable pageable);
}