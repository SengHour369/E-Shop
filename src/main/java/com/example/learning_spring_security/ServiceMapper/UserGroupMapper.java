package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Model.UserGroup;
import com.example.learning_spring_security.dto.Response.UserGroupResponse;

public class UserGroupMapper {

    public static UserGroupResponse toResponse(UserGroup ug) {
        return UserGroupResponse.builder()
                .groupId(ug.getGroupId())
                .isActive(ug.getIsActive())
                .createdAt(ug.getCreatedAt())
                .updatedAt(ug.getUpdatedAt())
                .build();
    }

    public static UserGroup toEntity(Long userId, Long groupId,String display) {
        return UserGroup.builder()
                .userId(userId)
                .groupId(groupId)
                .isActive(true)
                .isDelete(false)
                .build();
    }
}