package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.GetFunctionPermissionRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

public interface FunctionPermissionService {

    // Paginated – skip (or cache with allEntries eviction)
    ResponseErrorTemplate getFunctions(GetFunctionPermissionRequest request);

    @Cacheable(value = "functions", key = "#funcId")
    ResponseErrorTemplate getFunctionById(Long funcId);

    @CacheEvict(value = "functions", allEntries = true)
    ResponseErrorTemplate createFunction(String funcCode, String funcName, String description, String module);

    @CacheEvict(value = "functions", allEntries = true)
    ResponseErrorTemplate updateFunction(Long funcId, String funcName, String description, Boolean isActive);

    @CacheEvict(value = "functions", allEntries = true)
    ResponseErrorTemplate deleteFunction(Long funcId);
}