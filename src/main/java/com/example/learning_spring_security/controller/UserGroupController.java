package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.Security.RequirePermission;
import com.example.learning_spring_security.Service.ServiceStructure.UserGroupService;
import com.example.learning_spring_security.dto.Request.GetUserGroupRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user-groups")
@RequiredArgsConstructor
public class UserGroupController extends BaseController {

    private final UserGroupService userGroupService;

    @PostMapping("/get/all")
    public ResponseEntity<ResponseErrorTemplate> getUserGroups(
            @RequestBody GetUserGroupRequest request) {
        ResponseErrorTemplate response = userGroupService.getUserGroups(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/get/id/")
    public ResponseEntity<ResponseErrorTemplate> getUserGroupById(@RequestParam Long id) {
        ResponseErrorTemplate response = userGroupService.getUserGroupById(id);
        return ResponseEntity.ok(response);
    }

    @RequirePermission(funcId = 102L)
    @PostMapping("/create/")
    public ResponseEntity<ResponseErrorTemplate> createUserGroup(
            @RequestParam String groupCode,
            @RequestParam String groupName,
            @RequestParam(required = false) String display) {
        ResponseErrorTemplate response = userGroupService.createUserGroup(groupCode, groupName, display);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @RequirePermission(funcId = 103L)
    @PostMapping("/update/")
    public ResponseEntity<ResponseErrorTemplate> updateUserGroup(
            @RequestParam Long id,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) String display,
            @RequestParam(required = false) Boolean isActive) {
        ResponseErrorTemplate response = userGroupService.updateUserGroup(id, groupName, display, isActive);
        return ResponseEntity.ok(response);
    }

    @RequirePermission(funcId = 104L)
    @PostMapping("/delete/")
    public ResponseEntity<ResponseErrorTemplate> deleteUserGroup(@RequestParam Long id) {
        ResponseErrorTemplate response = userGroupService.deleteUserGroup(id);
        return ResponseEntity.ok(response);
    }
}