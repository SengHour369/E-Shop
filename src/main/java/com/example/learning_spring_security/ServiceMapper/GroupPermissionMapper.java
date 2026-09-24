package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Model.GroupPermission;
import com.example.learning_spring_security.dto.Response.GroupPermissionResponse;

public class GroupPermissionMapper {

    public static GroupPermissionResponse toResponse(GroupPermission permission) {
        return GroupPermissionResponse.builder()
                .groupPermissionId(permission.getGroupPermissionId())
                .groupId(permission.getGroupId())
                .funcId(permission.getFuncId())
                .isActive(permission.getIsActive())
                .createdAt(permission.getCreatedAt())
                .updatedAt(permission.getUpdatedAt())
                .build();
    }

    public static GroupPermission toEntity(Long groupId, Long funcId) {
        return GroupPermission.builder()
                .groupId(groupId)
                .funcId(funcId)
                .isActive(true)
                .build();
    }
}