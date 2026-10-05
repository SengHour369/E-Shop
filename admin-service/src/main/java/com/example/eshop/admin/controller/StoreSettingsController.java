package com.example.eshop.admin.controller;

import com.example.eshop.admin.dto.StoreSettingsRequest;
import com.example.eshop.admin.service.StoreSettingsService;
import com.example.eshop.common.dto.APIResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.DateTimeException;
import java.time.ZoneId;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/settings")
@PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
public class StoreSettingsController {
    private final StoreSettingsService service;

    @GetMapping
    public APIResponse<StoreSettingsRequest> current() {
        return APIResponse.success("Store settings", 200, service.current());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<APIResponse<StoreSettingsRequest>> save(@Valid @RequestBody StoreSettingsRequest request) {
        try {
            ZoneId.of(request.timezone());
        } catch (DateTimeException exception) {
            return ResponseEntity.badRequest().body(APIResponse.error("Unknown timezone", 400));
        }
        return ResponseEntity.ok(APIResponse.success("Store settings saved", 200, service.save(request)));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse<Void>> invalid(org.springframework.web.bind.MethodArgumentNotValidException exception) {
        var error = exception.getBindingResult().getFieldErrors().get(0);
        return ResponseEntity.badRequest().body(APIResponse.error(error.getField() + ": " + error.getDefaultMessage(), 400));
    }
}
