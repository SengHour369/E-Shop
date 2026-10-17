package com.example.eshop.auth.controller;
import com.example.eshop.auth.service.impl.AuthServiceImpl;
import com.example.eshop.auth.dto.response.AuthenticationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class SecondFactorController {
    private final AuthServiceImpl auth;
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<AuthenticationResponse> error(org.springframework.web.server.ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).cacheControl(CacheControl.noStore()).body(AuthenticationResponse.builder().message(error.getReason()).build());
    }
    public record Code(@NotBlank String challenge,@Pattern(regexp="[0-9]{6}") @NotBlank String code){}
    @PostMapping("/second-factor") public ResponseEntity<AuthenticationResponse> verify(@Valid @RequestBody Code code) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(auth.verifySecondFactor(code.challenge(),code.code()));
    }
}
