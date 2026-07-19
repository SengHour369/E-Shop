package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.GetUserPermissionRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

public interface UserPermissionService {

    // Paginated – skip
    ResponseErrorTemplate getUserPermissions(GetUserPermissionRequest request);

    @Cacheable(value = "userPermissions", key = "#userPermissionId")
    ResponseErrorTemplate getUserPermissionById(Long userPermissionId);

    @CacheEvict(value = "userPermissions", allEntries = true)
    ResponseErrorTemplate createUserPermission(Long userId, Long funcId);

    @CacheEvict(value = "userPermissions", key = "#userPermissionId")
    ResponseErrorTemplate updateUserPermission(Long userPermissionId, Boolean isActive);

    @CacheEvict(value = "userPermissions", key = "#userPermissionId")
    ResponseErrorTemplate deleteUserPermission(Long userPermissionId);
}