package com.example.eshop.ai;

import com.example.eshop.ai.workflow.*;
import com.example.eshop.common.audit.AuditLogService;
import com.example.eshop.common.automation.CommerceEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageRequest;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

// Persistence and dispatch use the real store/database; audit delivery is isolated from the test.
@DataJpaTest(properties={"spring.jpa.hibernate.ddl-auto=create-drop","spring.datasource.url=jdbc:h2:mem:workflow_triggers;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password="})
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@Import(WorkflowTriggerStore.class)
class WorkflowTriggerIntegrationTest {
 @Autowired WorkflowTriggerStore store;@Autowired WorkflowTriggerRepository triggers;@Autowired WorkflowTriggerJobRepository jobs;@MockitoBean AuditLogService audit;
 WorkflowTrigger trigger(String type,long owner){var t=new WorkflowTrigger();t.setId(UUID.randomUUID());t.setWorkflowId(UUID.randomUUID());t.setVersionId(UUID.randomUUID());t.setOwnerId(owner);t.setTriggerType(type);t.setState("ACTIVE");t.setGrantId(UUID.randomUUID());t.setAuthorizationExpiresAt(Instant.now().plusSeconds(86400));t.setActivatedAt(Instant.now().minusSeconds(5));return t;}
 @Test void onceQueuesExactlyOnceAndCanBeClaimedAfterDispatch(){var t=trigger("SCHEDULED",1);t.setNextRunAt(Instant.now().minusSeconds(2));t.setIntervalMinutes(0);store.activate(t);store.enqueueDue();store.enqueueDue();assertThat(jobs.count()).isEqualTo(1);assertThat(triggers.findById(t.getId()).orElseThrow().getState()).isEqualTo("COMPLETED");var job=store.claim();assertThat(job.getStatus()).isEqualTo("RUNNING");store.finish(job.getId(),"SUCCEEDED",null,false);assertThat(store.claim()).isNull();assertThat(jobs.findById(job.getId()).orElseThrow().getStatus()).isEqualTo("SUCCEEDED");}
 @Test void recurringSchedulesCoalesceMissedIntervals(){var t=trigger("SCHEDULED",1);t.setNextRunAt(Instant.now().minusSeconds(7200));t.setIntervalMinutes(1);store.activate(t);store.enqueueDue();store.enqueueDue();assertThat(jobs.count()).isEqualTo(1);assertThat(triggers.findById(t.getId()).orElseThrow().getNextRunAt()).isAfter(Instant.now());}
 @Test void realCustomSignalsAreOwnerScopedDeduplicatedAndPauseCancelsQueuedJobs(){var a=trigger("EVENT",1);a.setEventType("CUSTOM");a.setEventName("catalog.check");store.activate(a);var b=trigger("EVENT",2);b.setEventType("CUSTOM");b.setEventName("catalog.check");store.activate(b);var key=UUID.randomUUID();store.signal(1,"catalog.check",key);store.signal(1,"catalog.check",key);assertThat(jobs.count()).isEqualTo(1);assertThat(jobs.findByTriggerIdAndStatus(a.getId(),"QUEUED")).hasSize(1);assertThat(jobs.findByTriggerIdAndStatus(b.getId(),"QUEUED")).isEmpty();store.pause(a.getId(),1);assertThat(store.claim()).isNull();store.signal(1,"catalog.check",UUID.randomUUID());assertThat(jobs.count()).isEqualTo(1);assertThatThrownBy(()->store.pause(a.getId(),2)).hasMessageContaining("404");}
 @Test void expiredAuthorizationCannotQueue(){var t=trigger("EVENT",1);t.setEventType("CUSTOM");t.setEventName("catalog.check");t.setAuthorizationExpiresAt(Instant.now().minusSeconds(1));store.activate(t);store.enqueueDue();store.signal(1,"catalog.check",UUID.randomUUID());assertThat(jobs.count()).isZero();assertThat(triggers.findById(t.getId()).orElseThrow().getState()).isEqualTo("AUTHORIZATION_EXPIRED");}
 @Test void interruptedJobsAreUnknownAndNeverReplayed(){var t=trigger("EVENT",1);t.setEventType("CUSTOM");t.setEventName("catalog.check");store.activate(t);store.signal(1,"catalog.check",UUID.randomUUID());var j=store.claim();j=jobs.findById(j.getId()).orElseThrow();j.setStartedAt(Instant.now().minusSeconds(901));jobs.saveAndFlush(j);store.recover();assertThat(jobs.findById(j.getId()).orElseThrow().getStatus()).isEqualTo("UNKNOWN");assertThat(store.claim()).isNull();}
 @Test void commerceSubscriptionsHonorActivationTimeOrderFilterAndEventIdentity(){var t=trigger("EVENT",1);t.setEventType("ORDER_STATUS_CHANGED");t.setOrderId(42L);store.activate(t);var now=Instant.now();java.util.function.BiFunction<Long,Instant,CommerceEvent> event=(order,time)->new CommerceEvent(1,UUID.randomUUID(),CommerceEvent.Type.ORDER_STATUS_CHANGED,order,1,java.math.BigDecimal.ZERO,"USD","COD",null,"workflow-commerce-test",null,time);store.commerce(event.apply(43L,now));store.commerce(event.apply(42L,now.minusSeconds(3600)));assertThat(jobs.count()).isZero();var matching=event.apply(42L,now);store.commerce(matching);store.commerce(matching);assertThat(jobs.count()).isEqualTo(1);assertThat(jobs.findByTriggerIdAndStatus(t.getId(),"QUEUED")).hasSize(1);}
 @Test void decodesTheActualAuthorizationServiceGrantContract()throws Exception{var mapper=new ObjectMapper().findAndRegisterModules().enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);var id=UUID.randomUUID();var value=mapper.readValue("{\"id\":\""+id+"\",\"ownerId\":1,\"versionId\":\""+UUID.randomUUID()+"\",\"expiresAt\":\"2026-10-26T03:00:00Z\",\"revoked\":false,\"createdAt\":\"2026-10-25T03:00:00Z\"}",WorkflowAuthorizationClient.Grant.class);assertThat(value.id()).isEqualTo(id);assertThat(value.expiresAt()).isAfter(value.createdAt());}
 @Test void triggerPolicyRejectsInvalidConfiguration()throws Exception{var json=new ObjectMapper();assertThatThrownBy(()->WorkflowTriggerPolicy.validate(json.readTree("{\"triggerType\":\"SCHEDULED\",\"trigger\":{\"validForDays\":7,\"firstRunAt\":\"invalid\",\"intervalMinutes\":1}}"))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);WorkflowTriggerPolicy.validate(json.readTree("{\"triggerType\":\"EVENT\",\"trigger\":{\"validForDays\":7,\"eventType\":\"CUSTOM\",\"eventName\":\"catalog.check\"}}"));}
}
