package com.example.eshop.auth.service;

import com.example.eshop.auth.dto.request.GetGroupPermissionRequest;
import com.example.eshop.auth.dto.response.ResponseErrorTemplate;

public interface GroupPermissionService {
    ResponseErrorTemplate getGroupPermissions(GetGroupPermissionRequest request);
    ResponseErrorTemplate getGroupPermissionById(Long groupPermissionId);
    ResponseErrorTemplate createGroupPermission(Long groupId, Long funcId);
    ResponseErrorTemplate updateGroupPermission(Long groupPermissionId, Boolean isActive);
    ResponseErrorTemplate deleteGroupPermission(Long groupPermissionId);
}