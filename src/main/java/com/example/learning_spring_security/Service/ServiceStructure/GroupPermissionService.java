package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.GetGroupPermissionRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;

public interface GroupPermissionService {
    ResponseErrorTemplate getGroupPermissions(GetGroupPermissionRequest request);
    ResponseErrorTemplate getGroupPermissionById(Long groupPermissionId);
    ResponseErrorTemplate createGroupPermission(Long groupId, Long funcId);
    ResponseErrorTemplate updateGroupPermission(Long groupPermissionId, Boolean isActive);
    ResponseErrorTemplate deleteGroupPermission(Long groupPermissionId);
}