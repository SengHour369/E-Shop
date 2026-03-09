package com.example.learning_spring_security.controller;


import com.example.learning_spring_security.Model.User;
import com.example.learning_spring_security.Service.ServiceStructure.UserService;
import com.example.learning_spring_security.dto.Request.UserRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("api/v1/user")
@Slf4j
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")

public class UserController {
   private final UserService userService;
   @PostMapping(value = "/{id}", consumes = MediaType.ALL_VALUE)
   @PreAuthorize("hasRole('ADMIN')")
   @Operation(summary = "Change user status for admin")
   public ResponseEntity<ResponseErrorTemplate> updateUserStatus(@PathVariable Long id,
                                                                 @RequestParam String status) {
        ResponseErrorTemplate responseErrorTemplate = this.userService.changeUserStatus(id,status);
        return ResponseEntity.ok(responseErrorTemplate);
   }
   @PostMapping(value = "/{id}/image",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
   @Operation(summary =  "Update image user for user")
   public  ResponseEntity<ResponseErrorTemplate> updateUserImage(@PathVariable Long id,
                                                                 @RequestPart("file") MultipartFile file) {
       ResponseErrorTemplate responseErrorTemplate = this.userService.updateProfilePicture(id, file);
       return ResponseEntity.ok(responseErrorTemplate);
   }
    @GetMapping(value = "/{id}/user")
    @Operation(summary = " Output By Id user ")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseErrorTemplate> getUserByIdAllUser(@PathVariable Long id) {
      ResponseErrorTemplate responseErrorTemplate =  this.userService.getUserById(id);
       return ResponseEntity.ok(responseErrorTemplate);
   }
    @GetMapping("/All")
    @Operation(summary = " Output all admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ResponseErrorTemplate>> getAllUser() {
        List<ResponseErrorTemplate> responseErrorTemplate =  this.userService.getAllUsers();
        return ResponseEntity.ok(responseErrorTemplate);
    }
    @PutMapping(value = "/{id}/update")
    @Operation(summary = "update by admin ")
   // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseErrorTemplate> updateUser(@PathVariable Long id, @RequestBody UserRequest userRequest) {
       ResponseErrorTemplate responseErrorTemplate = this.userService.updateUser(id,userRequest);
       return ResponseEntity.ok(responseErrorTemplate);
    }
    @DeleteMapping(value = "/{id}/delete")
    @Operation(summary ="delete by admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseErrorTemplate> deleteUser(@PathVariable Long id) {
       this.userService.deleteUser(id);
      return null;
    }
    @GetMapping("/count")
    @Operation(summary = "count user by admin")
    @PreAuthorize("hasRole('ADMIN')")
    public  ResponseEntity<Long> getAllCountUser() {
        Long responseErrorTemplate = this.userService.countUsers();
        return ResponseEntity.ok(responseErrorTemplate);

    }

}
