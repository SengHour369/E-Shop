package com.example.eshop.auth.service;

import com.example.eshop.auth.dto.request.GetUserGroupRequest;
import com.example.eshop.auth.dto.response.ResponseErrorTemplate;

public interface UserGroupService {
    ResponseErrorTemplate getUserGroups(GetUserGroupRequest request);
    ResponseErrorTemplate getUserGroupById(Long groupId);
    ResponseErrorTemplate createUserGroup(String groupCode, String groupName, String display);
    ResponseErrorTemplate updateUserGroup(Long groupId, String groupName, String display, Boolean isActive);
    ResponseErrorTemplate deleteUserGroup(Long groupId);
}