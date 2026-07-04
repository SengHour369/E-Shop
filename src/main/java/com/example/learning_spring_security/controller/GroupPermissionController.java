package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.Security.RequirePermission;
import com.example.learning_spring_security.Service.ServiceStructure.GroupPermissionService;
import com.example.learning_spring_security.dto.Request.GetGroupPermissionRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/group-permissions")
@RequiredArgsConstructor
public class GroupPermissionController extends BaseController {

    private final GroupPermissionService groupPermissionService;

    @PostMapping("/get/all")
    public ResponseEntity<ResponseErrorTemplate> getGroupPermissions(
            @RequestBody GetGroupPermissionRequest request) {
        ResponseErrorTemplate response = groupPermissionService.getGroupPermissions(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/get/id/")
    public ResponseEntity<ResponseErrorTemplate> getGroupPermissionById(@RequestParam Long id) {
        ResponseErrorTemplate response = groupPermissionService.getGroupPermissionById(id);
        return ResponseEntity.ok(response);
    }

    @RequirePermission(funcId = 301L)
    @PostMapping("/create/")
    public ResponseEntity<ResponseErrorTemplate> createGroupPermission(
            @RequestParam Long groupId,
            @RequestParam Long funcId) {
        ResponseErrorTemplate response = groupPermissionService.createGroupPermission(groupId, funcId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @RequirePermission(funcId = 302L)
    @PostMapping("/update/")
    public ResponseEntity<ResponseErrorTemplate> updateGroupPermission(
            @RequestParam Long id,
            @RequestParam Boolean isActive) {
        ResponseErrorTemplate response = groupPermissionService.updateGroupPermission(id, isActive);
        return ResponseEntity.ok(response);
    }

    @RequirePermission(funcId = 303L)
    @PostMapping("/delete/")
    public ResponseEntity<ResponseErrorTemplate> deleteGroupPermission(@RequestParam Long id) {
        ResponseErrorTemplate response = groupPermissionService.deleteGroupPermission(id);
        return ResponseEntity.ok(response);
    }
}