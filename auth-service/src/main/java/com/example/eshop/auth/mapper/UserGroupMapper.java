package com.example.eshop.auth.mapper;

import com.example.eshop.auth.model.UserGroup;
import com.example.eshop.auth.dto.response.UserGroupResponse;

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