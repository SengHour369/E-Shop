package com.example.eshop.auth.controller;

import com.example.eshop.auth.service.impl.UserEditorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController
@RequestMapping("/api/v1/user")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class UserEditorController {

    private final UserEditorService editor;
    private final com.example.eshop.auth.service.impl.UserDetailsService details;

    @GetMapping("/{id}/details")
    public Map<String, Object> details(@PathVariable Long id) {
        return details.get(id);
    }

    @GetMapping("/{id}/details/activity")
    public Map<String, Object> activity(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page
    ) {
        return details.activity(id, page);
    }

    public record Update(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 100) String username,
            @NotBlank @Size(max = 30) String phone,
            String birthdate,
            @Size(max = 100) String jobTitle,
            @Size(max = 100) String department,
            Long managerId,
            String gender,
            @Size(max = 255) String address,
            @NotBlank String language,
            @NotNull Boolean active,
            @NotEmpty List<Long> roleIds,
            @NotEmpty List<Long> groupIds,
            @NotNull Boolean twoFactorRequired,
            @Positive Long approvalReviewerId,
            List<@Positive Long> permissionIds
    ) {}

    @GetMapping("/editor-options")
    public Map<String, Object> options() {
        return editor.options();
    }

    @GetMapping("/{id}/editor")
    public Map<String, Object> get(@PathVariable Long id) {
        return editor.get(id);
    }

    @PutMapping("/{id}/editor")
    public Map<String, Object> update(
            @PathVariable Long id,
            @Valid @RequestBody Update body
    ) {
        return editor.update(id, body);
    }

    @PostMapping(value = "/{id}/editor/photo", consumes = "multipart/form-data")
    public Map<String, Object> photo(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file
    ) {
        return editor.photo(id, file);
    }
}