//package com.example.learning_spring_security.controller;
//
//import com.example.learning_spring_security.Security.RequirePermission;
//import com.example.learning_spring_security.Service.ServiceStructure.UserPermissionService;
//import com.example.learning_spring_security.dto.Request.GetUserPermissionRequest;
//import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/v1/user-permissions")
//@RequiredArgsConstructor
//public class UserPermissionController extends BaseController {
//
//    private final UserPermissionService userPermissionService;
//
//    @PostMapping("/get/all")
//    public ResponseEntity<ResponseErrorTemplate> getUserPermissions(
//            @RequestBody GetUserPermissionRequest request) {
//        ResponseErrorTemplate response = userPermissionService.getUserPermissions(request);
//        return ResponseEntity.ok(response);
//    }
//
//    @PostMapping("/get/id/")
//    public ResponseEntity<ResponseErrorTemplate> getUserPermissionById(@RequestParam Long id) {
//        ResponseErrorTemplate response = userPermissionService.getUserPermissionById(id);
//        return ResponseEntity.ok(response);
//    }
//
//    @RequirePermission(funcId = 201L)
//    @PostMapping("/create/")
//    public ResponseEntity<ResponseErrorTemplate> createUserPermission(
//            @RequestParam Long userId,
//            @RequestParam Long funcId) {
//        ResponseErrorTemplate response = userPermissionService.createUserPermission(userId, funcId);
//        return new ResponseEntity<>(response, HttpStatus.CREATED);
//    }
//
//    @RequirePermission(funcId = 202L)
//    @PostMapping("/update/")
//    public ResponseEntity<ResponseErrorTemplate> updateUserPermission(
//            @RequestParam Long id,
//            @RequestParam Boolean isActive) {
//        ResponseErrorTemplate response = userPermissionService.updateUserPermission(id, isActive);
//        return ResponseEntity.ok(response);
//    }
//
//    @RequirePermission(funcId = 203L)
//    @PostMapping("/delete/")
//    public ResponseEntity<ResponseErrorTemplate> deleteUserPermission(@RequestParam Long id) {
//        ResponseErrorTemplate response = userPermissionService.deleteUserPermission(id);
//        return ResponseEntity.ok(response);
//    }
//}