package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.Service.ServiceStructure.FunctionPermissionService;
import com.example.learning_spring_security.dto.Request.GetFunctionPermissionRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/functions")
@RequiredArgsConstructor
public class FunctionPermissionController extends BaseController {

    private final FunctionPermissionService functionPermissionService;

    @PostMapping("/get/all")
    public ResponseEntity<ResponseErrorTemplate> getFunctions(
            @RequestBody GetFunctionPermissionRequest request) {
        ResponseErrorTemplate response = functionPermissionService.getFunctions(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/get/id/")
    public ResponseEntity<ResponseErrorTemplate> getFunctionById(@RequestParam Long id) {
        ResponseErrorTemplate response = functionPermissionService.getFunctionById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create/")
    public ResponseEntity<ResponseErrorTemplate> createFunction(
            @RequestParam String funcCode,
            @RequestParam String funcName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String module) {
        ResponseErrorTemplate response = functionPermissionService.createFunction(funcCode, funcName, description, module);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/update/")
    public ResponseEntity<ResponseErrorTemplate> updateFunction(
            @RequestParam Long id,
            @RequestParam(required = false) String funcName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Boolean isActive) {
        ResponseErrorTemplate response = functionPermissionService.updateFunction(id, funcName, description, isActive);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/delete/")
    public ResponseEntity<ResponseErrorTemplate> deleteFunction(@RequestParam Long id) {
        ResponseErrorTemplate response = functionPermissionService.deleteFunction(id);
        return ResponseEntity.ok(response);
    }
}