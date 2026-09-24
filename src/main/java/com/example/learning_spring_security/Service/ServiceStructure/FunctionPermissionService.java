package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.GetFunctionPermissionRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;

public interface FunctionPermissionService {
    ResponseErrorTemplate getFunctions(GetFunctionPermissionRequest request);
    ResponseErrorTemplate getFunctionById(Long funcId);
    ResponseErrorTemplate createFunction(String funcCode, String funcName, String description, String module);
    ResponseErrorTemplate updateFunction(Long funcId, String funcName, String description, Boolean isActive);
    ResponseErrorTemplate deleteFunction(Long funcId);
}