package com.example.eshop.common.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.security.core.Authentication;

@Service @RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository repository;
    private final AuditContextProvider context;
    private final AuditSnapshots snapshots;

    /** The business mutation and its audit row become visible in the same commit. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuditEvent event) {
        repository.save(AuditLog.from(event, context.capture(), snapshots));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordForActor(AuditEvent event, Authentication actor) {
        repository.save(AuditLog.from(event, context.capture(actor), snapshots));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSecurity(AuditAction action, AuditResult result, Authentication actor, String errorCode) {
        var metadata = context.capture(actor);
        repository.save(AuditLog.from(new AuditEvent(action, "AUTHENTICATION", metadata.actorId(),
                null, null, result, errorCode), metadata, snapshots));
    }
}
