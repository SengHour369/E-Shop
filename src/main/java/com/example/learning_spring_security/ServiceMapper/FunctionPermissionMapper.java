package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Model.FunctionPermission;
import com.example.learning_spring_security.dto.Response.FunctionPermissionResponse;

public class FunctionPermissionMapper {

    public static FunctionPermissionResponse toResponse(FunctionPermission function) {
        return FunctionPermissionResponse.builder()
                .funcId(function.getFuncId())
                .funcCode(function.getFuncCode())
                .funcName(function.getFuncName())
                .description(function.getDescription())
                .module(function.getModule())
                .isActive(function.getIsActive())
                .createdAt(function.getCreatedAt())
                .updatedAt(function.getUpdatedAt())
                .build();
    }

    public static FunctionPermission toEntity(String funcCode, String funcName,
                                              String description, String module) {
        return FunctionPermission.builder()
                .funcCode(funcCode)
                .funcName(funcName)
                .description(description)
                .module(module)
                .isActive(true)
                .build();
    }
}