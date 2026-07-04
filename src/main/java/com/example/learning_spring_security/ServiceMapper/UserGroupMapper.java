package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Model.UserGroup;
import com.example.learning_spring_security.dto.Response.UserGroupResponse;

public class UserGroupMapper {

    public static UserGroupResponse toResponse(UserGroup group) {
        return UserGroupResponse.builder()
                .groupId(group.getGroupId())
                .groupCode(group.getGroupCode())
                .groupName(group.getGroupName())
                .display(group.getDisplay())
                .isActive(group.getIsActive())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }

    public static UserGroup toEntity(String groupCode, String groupName, String display) {
        return UserGroup.builder()
                .groupCode(groupCode)
                .groupName(groupName)
                .display(display)
                .isActive(false)
                .isDelete(false)
                .build();
    }
}