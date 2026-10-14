package com.example.eshop.auth.controller;

import com.example.eshop.auth.model.User;
import com.example.eshop.auth.repository.UserRepository;
import com.example.eshop.common.security.CurrentActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;

/** Customer self-service: identity comes exclusively from the verified security context. */
@RestController
@RequestMapping("/api/v1/user/me/profile")
@RequiredArgsConstructor
public class CustomerProfileController {
    private final UserRepository users;
    public record Profile(Long id, String username, String email, String fullName, String phone, String birthdate, String status) {}
    public record Update(@NotBlank @Size(max=150) String fullName,
                         @NotBlank @Pattern(regexp="\\+?[0-9 ()-]{7,25}") String phone,
                         @Size(max=10) String birthdate) {}
    private User owner() {
        return users.findById(CurrentActor.userId()).filter(u -> !Boolean.TRUE.equals(u.getDeleted()))
            .filter(User::isEnabled).filter(u -> "ACT".equals(u.getStatus()))
            .orElseThrow(() -> new AccessDeniedException("Account unavailable"));
    }
    private Profile view(User u) { return new Profile(u.getId(),u.getUsername(),u.getEmail(),u.getFullName(),u.getPhone(),u.getBirthdate(),u.getStatus()); }
    @GetMapping @Transactional(readOnly=true)
    public Profile get() { return view(owner()); }
    @PutMapping @Transactional
    public Profile update(@Valid @RequestBody Update request) {
        String date=request.birthdate();
        if(date!=null && !date.isBlank()) {
            try { if(LocalDate.parse(date).isAfter(LocalDate.now())) throw new IllegalArgumentException(); }
            catch(RuntimeException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter a valid birth date in the past"); }
        }
        User u=owner();
        u.setFullName(request.fullName().trim());u.setPhone(request.phone().trim());
        u.setBirthdate(date==null || date.isBlank()?null:date);
        try { return view(users.saveAndFlush(u)); }
        catch(org.springframework.dao.DataIntegrityViolationException e) { throw new ResponseStatusException(HttpStatus.CONFLICT,"Phone number is already in use"); }
    }
}
