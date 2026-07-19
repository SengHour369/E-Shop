package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.GetGroupPermissionRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

public interface GroupPermissionService {

    // Paginated – skip
    ResponseErrorTemplate getGroupPermissions(GetGroupPermissionRequest request);

    @Cacheable(value = "permissions", key = "#groupPermissionId")
    ResponseErrorTemplate getGroupPermissionById(Long groupPermissionId);

    @CacheEvict(value = {"permissions", "groups"}, allEntries = true)
    ResponseErrorTemplate createGroupPermission(Long groupId, Long funcId);

    @CacheEvict(value = {"permissions", "groups"}, allEntries = true)
    ResponseErrorTemplate updateGroupPermission(Long groupPermissionId, Boolean isActive);

    @CacheEvict(value = {"permissions", "groups"}, allEntries = true)
    ResponseErrorTemplate deleteGroupPermission(Long groupPermissionId);
}