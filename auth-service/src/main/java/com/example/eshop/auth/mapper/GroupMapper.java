package com.example.eshop.auth.mapper;

import com.example.eshop.auth.model.Group;
import com.example.eshop.auth.dto.response.GroupResponse;

public class GroupMapper {

    public static GroupResponse toResponse(Group group) {
        return GroupResponse.builder()
                .groupId(group.getId())
                .groupCode(group.getGroupCode())
                .groupName(group.getName())
                .description(group.getDescription())
                .status(group.getStatus())
                .type(group.getType())
                .isActive(group.getIsActive())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }

    public static Group toEntity(String groupCode, String name, String description, String status,String type) {
        return Group.builder()
                .groupCode(groupCode)
                .name(name)
                .description(description)
                .status(status)
                .type(type)
                .isActive(false)
                .isDelete(false)
                .build();
    }
}