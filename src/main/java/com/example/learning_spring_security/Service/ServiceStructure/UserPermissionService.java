package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.GetUserPermissionRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import com.example.learning_spring_security.dto.Response.UserPermissionPageResponse;

public interface UserPermissionService {
    ResponseErrorTemplate getUserPermissions(GetUserPermissionRequest request);
    ResponseErrorTemplate getUserPermissionById(Long userPermissionId);
    ResponseErrorTemplate createUserPermission(Long userId, Long funcId);
    ResponseErrorTemplate updateUserPermission(Long userPermissionId, Boolean isActive);
    ResponseErrorTemplate deleteUserPermission(Long userPermissionId);
}