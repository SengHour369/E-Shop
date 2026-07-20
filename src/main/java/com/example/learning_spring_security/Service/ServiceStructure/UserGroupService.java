package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.GetUserGroupRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;

public interface UserGroupService {
    ResponseErrorTemplate getUserGroups(GetUserGroupRequest request);
    ResponseErrorTemplate getUserGroupById(Long groupId);
    ResponseErrorTemplate createUserGroup(String groupCode, String groupName, String display);
    ResponseErrorTemplate updateUserGroup(Long groupId, String groupName, String display, Boolean isActive);
    ResponseErrorTemplate deleteUserGroup(Long groupId);
}