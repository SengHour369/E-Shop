package com.example.eshop.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserCreatedMail {
    public record Event(Long userId, String email, String name, String username) {}
    private final EmailNotificationService email;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void welcome(Event event) {
        if (event.email() == null || event.email().isBlank()) return;
        try {
            email.sendSimpleEmail(event.email(), "Welcome to E-Shop",
                    "Hello " + (event.name() == null ? event.username() : event.name()) + ",\n\n"
                    + "An administrator has created your E-Shop account.\n"
                    + "Your username is: " + event.username() + "\n\n"
                    + "Use the sign-in details provided by your administrator. "
                    + "If you need a new password, use Forgot Password on the sign-in page.\n\n"
                    + "If you did not expect this account, please contact the shop administrator.");
        } catch (RuntimeException failure) {
            // The account is committed; an SMTP failure must not cause duplicate creation retries.
            log.error("Welcome email failed for newly created user {}", event.userId(), failure);
        }
    }
}
