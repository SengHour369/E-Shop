package com.example.eshop.auth.service;

import com.example.eshop.auth.dto.request.GetUserPermissionRequest;
import com.example.eshop.auth.dto.response.ResponseErrorTemplate;
import com.example.eshop.auth.dto.response.UserPermissionPageResponse;

public interface UserPermissionService {
    ResponseErrorTemplate getUserPermissions(GetUserPermissionRequest request);
    ResponseErrorTemplate getUserPermissionById(Long userPermissionId);
    ResponseErrorTemplate createUserPermission(Long userId, Long funcId);
    ResponseErrorTemplate updateUserPermission(Long userPermissionId, Boolean isActive);
    ResponseErrorTemplate deleteUserPermission(Long userPermissionId);
}