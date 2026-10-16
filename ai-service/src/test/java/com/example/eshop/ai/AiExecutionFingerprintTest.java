package com.example.eshop.ai;

import com.example.eshop.ai.service.AiExecutionService;
import com.example.eshop.ai.model.AiExecution;
import com.example.eshop.ai.repository.*;
import com.example.eshop.common.audit.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.MDC;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiExecutionFingerprintTest {
    AiExecutionRepository rows = mock(AiExecutionRepository.class);
    AiExecutionService service = new AiExecutionService(rows, mock(NotificationOutboxRepository.class), mock(AuditLogService.class), new ObjectMapper());
    @BeforeEach void actor() {
        var auth = new UsernamePasswordAuthenticationToken("user",null,List.of());
        auth.setDetails(Map.of("userId",7L)); SecurityContextHolder.getContext().setAuthentication(auth); MDC.put("requestId","req_fingerprint");
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); MDC.clear(); }
    @Test void keyIsBoundToMessageAndOperationWithoutStoringRawMessage() {
        UUID id = UUID.randomUUID();
        AiExecution row = service.start(id,"execute:private customer message");
        assertThat(row.getRequestFingerprint()).matches("[a-f0-9]{64}").doesNotContain("private");
        when(rows.findById(id)).thenReturn(Optional.of(row));
        assertThat(service.existing(id,"execute:private customer message")).contains(row);
        assertThatThrownBy(()->service.existing(id,"execute:different message")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->service.existing(id,"confirm:"+id)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void legacyRowsRemainReadableButCannotBeReplayed() {
        UUID id=UUID.randomUUID();var row=new AiExecution();row.setId(id);row.setUserId(7);
        when(rows.findById(id)).thenReturn(Optional.of(row));
        assertThat(service.existing(id)).contains(row);
        assertThatThrownBy(()->service.existing(id,"execute:message")).isInstanceOf(ResponseStatusException.class);
    }
    @Test void anotherActorCannotReadOrReplayAnExecution() {
        UUID id=UUID.randomUUID();var row=new AiExecution();row.setId(id);row.setUserId(8);
        when(rows.findById(id)).thenReturn(Optional.of(row));
        assertThatThrownBy(()->service.existing(id,"execute:message")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
}
