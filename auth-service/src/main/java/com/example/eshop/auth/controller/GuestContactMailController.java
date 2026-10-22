package com.example.eshop.auth.controller;

import com.example.eshop.auth.service.impl.EmailService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

/** Fixed verification template only; no arbitrary subject, body or order data. */
@RestController @RequiredArgsConstructor @RequestMapping("/internal/commerce/guest/contact-code")
@PreAuthorize("hasAuthority('SERVICE_ORDER') and authentication.name == 'order-service'")
public class GuestContactMailController {
    private final EmailService mail;
    public record CodeMail(@NotBlank @Email @Size(max=254) String email,@Pattern(regexp="[0-9]{8}") @NotNull String code) { }
    @PostMapping public void send(@Valid @RequestBody CodeMail body) throws jakarta.mail.MessagingException {
        mail.sendVerificationEmail(body.email(),"Verify your E-Shop guest contact",
                "<p>Your guest checkout verification code is <strong>"+body.code()+"</strong>.</p><p>It expires in 5 minutes. If you did not request it, ignore this email.</p>");
    }
}
