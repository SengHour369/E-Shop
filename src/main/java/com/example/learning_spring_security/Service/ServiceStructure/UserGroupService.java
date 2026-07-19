package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.GetUserGroupRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

public interface UserGroupService {

    // Paginated – skip
    ResponseErrorTemplate getUserGroups(GetUserGroupRequest request);

    @Cacheable(value = "userGroups", key = "#groupId")
    ResponseErrorTemplate getUserGroupById(Long groupId);

    // Not used – throws exception
    ResponseErrorTemplate createUserGroup(String groupCode, String groupName, String display);

    @CacheEvict(value = "userGroups", key = "#groupId")
    ResponseErrorTemplate updateUserGroup(Long groupId, String groupName, String display, Boolean isActive);

    @CacheEvict(value = "userGroups", key = "#groupId")
    ResponseErrorTemplate deleteUserGroup(Long groupId);
}