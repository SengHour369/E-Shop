package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Model.UserPermission;
import com.example.learning_spring_security.dto.Response.UserPermissionResponse;

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