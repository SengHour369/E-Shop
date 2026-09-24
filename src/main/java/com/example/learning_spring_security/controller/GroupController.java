package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.Service.ServiceStructure.GroupService;
import com.example.learning_spring_security.dto.Request.GroupRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping("/create")
    public ResponseEntity<ResponseErrorTemplate> createGroup(@Valid @RequestBody GroupRequest request) {
        ResponseErrorTemplate response = groupService.createGroup(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    @GetMapping("/get/all")
    public ResponseEntity<ResponseErrorTemplate> getAllGroups(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        ResponseErrorTemplate response = groupService.getAllGroups(pageable);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/get/{id}")
    public ResponseEntity<ResponseErrorTemplate> getGroupById(@PathVariable Long id) {
        ResponseErrorTemplate response = groupService.getGroupById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ResponseErrorTemplate> updateGroup(
            @PathVariable Long id,
            @Valid @RequestBody GroupRequest request) {
        ResponseErrorTemplate response = groupService.updateGroup(id, request);
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ResponseErrorTemplate> deleteGroup(@PathVariable Long id) {
        ResponseErrorTemplate response = groupService.deleteGroup(id);
        return ResponseEntity.ok(response);
    }


    @PatchMapping("/activate/{id}")
    public ResponseEntity<ResponseErrorTemplate> toggleActive(
            @PathVariable Long id,
            @RequestParam Boolean isActive) {
        ResponseErrorTemplate response = groupService.toggleGroupActive(id, isActive);
        return ResponseEntity.ok(response);
    }
}