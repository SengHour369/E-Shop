package com.example.eshop.auth.controller;

import com.example.eshop.auth.service.AccountAccessService;
import com.example.eshop.auth.repository.UserRepository;
import com.example.eshop.common.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ImportReviewerController {

    private final UserRepository users;
    private final AccountAccessService access;

    @GetMapping("/internal/authorization/import-reviewers/{id}")
    @Transactional(readOnly = true)
    public boolean eligible(@PathVariable long id) {
        if (!access.snapshot(
                users.findById(CurrentActor.userId())
                        .orElseThrow(() -> new AccessDeniedException(
                                "Account unavailable"
                        ))
        ).permits("IMPORT_UPLOAD")) {
            throw new AccessDeniedException("Import permission required");
        }

        return users.findById(id)
                .filter(u ->
                        u.isEnabled()
                                && "ACT".equals(u.getStatus())
                                && !Boolean.TRUE.equals(u.getDeleted())
                )
                .filter(u ->
                        access.snapshot(u).permissions().containsAll(
                                java.util.Set.of(
                                        "IMPORT_REVIEW",
                                        "IMPORT_APPROVE",
                                        "IMPORT_EXECUTE"
                                )
                        )
                )
                .isPresent();
    }
}