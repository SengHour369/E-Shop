package com.example.eshop.auth.mapper;

import com.example.eshop.auth.model.UserPermission;
import com.example.eshop.auth.dto.response.UserPermissionResponse;

public class UserPermissionMapper {

    public static UserPermissionResponse toResponse(UserPermission permission) {
        return UserPermissionResponse.builder()
                .userPermissionId(permission.getUserPermissionId())
                .userId(permission.getUserId())
                .funcId(permission.getFuncId())
                .isActive(permission.getIsActive())
                .createdAt(permission.getCreatedAt())
                .updatedAt(permission.getUpdatedAt())
                .build();
    }

    public static UserPermission toEntity(Long userId, Long funcId) {
        return UserPermission.builder()
                .userId(userId)
                .funcId(funcId)
                .isActive(true)
                .build();
    }
}