package com.example.eshop.auth.service;

import com.example.eshop.auth.dto.request.GetFunctionPermissionRequest;
import com.example.eshop.auth.dto.response.ResponseErrorTemplate;

public interface FunctionPermissionService {
    ResponseErrorTemplate getFunctions(GetFunctionPermissionRequest request);
    ResponseErrorTemplate getFunctionById(Long funcId);
    ResponseErrorTemplate createFunction(String funcCode, String funcName, String description, String module);
    ResponseErrorTemplate updateFunction(Long funcId, String funcName, String description, Boolean isActive);
    ResponseErrorTemplate deleteFunction(Long funcId);
}