package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Model.Group;
import com.example.learning_spring_security.dto.Response.GroupResponse;

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