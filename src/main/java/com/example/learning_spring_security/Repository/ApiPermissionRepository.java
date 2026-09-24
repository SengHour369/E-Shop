package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.ApiPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApiPermissionRepository extends JpaRepository<ApiPermission, Long> {

    @Query("SELECT a FROM ApiPermission a WHERE a.method = :method AND a.isActive = true AND a.isDelete = false")
    List<ApiPermission> findActiveByMethod(@Param("method") String method);
    boolean existsByMethodAndApi(String method, String api);
}