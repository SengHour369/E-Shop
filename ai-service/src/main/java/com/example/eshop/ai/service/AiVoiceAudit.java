package com.example.eshop.ai.service;

import com.example.eshop.common.audit.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiVoiceAudit {
    private final AuditLogService audit;
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void accepted(String language, long bytes) {
        audit.record(AuditEvent.success(AuditAction.AI_REQUEST_ACCEPTED,"VOICE_TRANSCRIPTION",null,
                null,Map.of("intent","VOICE_TRANSCRIPTION","count",bytes)));
    }
}
