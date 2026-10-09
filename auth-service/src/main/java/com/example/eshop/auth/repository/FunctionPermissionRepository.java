package com.example.eshop.auth.repository;

import com.example.eshop.auth.model.FunctionPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FunctionPermissionRepository extends JpaRepository<FunctionPermission, Long> {

    @Query("""
            select distinct f.funcCode from FunctionPermission f
            where f.isActive = true and f.isDelete = false
              and (
                exists (select p.userPermissionId from UserPermission p
                        where p.userId = :userId and p.funcId = f.funcId
                          and p.isActive = true and p.isDelete = false)
                or exists (select gp.groupPermissionId from GroupPermission gp, UserGroup ug, Group g
                           where ug.userId = :userId and ug.groupId = gp.groupId
                             and g.id = ug.groupId and gp.funcId = f.funcId
                             and ug.isActive = true and ug.isDelete = false
                             and gp.isActive = true and gp.isDelete = false
                             and g.isActive = true and g.isDelete = false)
              )
            """)
    java.util.Set<String> effectiveCodes(@Param("userId") Long userId);

    Optional<FunctionPermission> findByFuncCodeAndIsDeleteFalse(String funcCode);

    @Query("SELECT COALESCE(MAX(f.funcId), 0) FROM FunctionPermission f")
    Long findMaxFuncId();

    @Query("SELECT COUNT(f) > 0 FROM FunctionPermission f WHERE f.funcCode = :funcCode AND f.isDelete = false")
    boolean existsByFuncCode(@Param("funcCode") String funcCode);

    @Query("SELECT f FROM FunctionPermission f WHERE f.isActive = :isActive AND f.isDelete = false")
    Page<FunctionPermission> findByIsActive(@Param("isActive") Boolean isActive, Pageable pageable);

    @Query("SELECT f FROM FunctionPermission f WHERE f.module = :module AND f.isDelete = false")
    Page<FunctionPermission> findByModule(@Param("module") String module, Pageable pageable);

    @Query("SELECT f FROM FunctionPermission f WHERE LOWER(f.funcName) LIKE LOWER(CONCAT('%', :name, '%')) AND f.isDelete = false")
    Page<FunctionPermission> findByFuncNameContaining(@Param("name") String name, Pageable pageable);

    @Query("SELECT f FROM FunctionPermission f WHERE LOWER(f.module) = LOWER(:module) AND f.isActive = :isActive AND f.isDelete = false")
    Page<FunctionPermission> findByModuleAndIsActive(@Param("module") String module,
                                                     @Param("isActive") Boolean isActive,
                                                     Pageable pageable);
}
