package com.example.eshop.order.guest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController @RequestMapping("/api/v1/guest/contact") @RequiredArgsConstructor
public class GuestContactController {
    private final GuestSessionService sessions;
    private final GuestContactService contact;
    public record Contact(@NotBlank @Email @Size(max=254) String email) { }
    public record Verification(@NotBlank @Email @Size(max=254) String email,@NotNull @Pattern(regexp="[0-9]{8}") String code) { }
    @PostMapping("/code") public void send(HttpServletRequest request,@Valid @RequestBody Contact body) {
        contact.send(sessions.require(request,true),body.email());
    }
    @PostMapping("/verify") public void verify(HttpServletRequest request,@Valid @RequestBody Verification body) {
        contact.verify(sessions.require(request,true),body.email(),body.code());
    }
}
