package com.example.eshop.ai;

import com.example.eshop.ai.workflow.*;
import com.example.eshop.ai.registry.AiToolRegistry;
import com.example.eshop.common.security.*;
import com.example.eshop.common.audit.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import java.util.*;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties={"spring.jpa.hibernate.ddl-auto=create-drop","spring.datasource.url=jdbc:h2:mem:workflow_lifecycle;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password="})
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@Import({WorkflowController.class,WorkflowPublicationService.class,WorkflowRunLedger.class,AiToolRegistry.class,ObjectMapper.class,AuditLogService.class,AuditContextProvider.class,AuditSnapshots.class})
class WorkflowLifecycleTest {
 @Autowired WorkflowController drafts;
 @Autowired WorkflowPublicationService publications;
 @Autowired WorkflowRunLedger ledger;
 @Autowired WorkflowRunRepository runs;
 @Autowired WorkflowRunStepRepository steps;
 @Autowired ObjectMapper mapper;
 @MockitoBean LivePermissionService permissions;

 @BeforeEach void actor(){when(permissions.current()).thenReturn(new PermissionSnapshot(101,true,Set.of()));}
 UUID begin() throws Exception{
  var body=mapper.readTree("{\"name\":\"Read product workflow\",\"description\":\"Read-only real tool\",\"module\":\"catalog-service\",\"triggerType\":\"MANUAL\",\"steps\":[{\"key\":\"one\",\"name\":\"Read product\",\"tool\":\"PRODUCT_GET\",\"parameters\":{\"id\":1}},{\"key\":\"two\",\"name\":\"Read another product\",\"tool\":\"PRODUCT_GET\",\"parameters\":{\"id\":2}}]}");
  var draft=drafts.create(new WorkflowController.Write(null,body));var version=publications.publish(draft.id(),draft.revision());var key=UUID.randomUUID();ledger.begin(version.getId(),key);return key;
 }
 @Test void cancellationPersistsUnknownForInflightAndSkipsPendingSteps()throws Exception{
  UUID id=begin();ledger.startStep(id,0);var cancelled=ledger.cancel(id);
  assertThat(cancelled.run().getStatus()).isEqualTo("CANCELLED");
  assertThat(cancelled.steps()).extracting(WorkflowRunStep::getStatus).containsExactly("UNKNOWN","SKIPPED");
  assertThat(cancelled.steps().get(0).getErrorCode()).isEqualTo("CANCELLED_DURING_REQUEST");
  ledger.finishStep(id,0,"SUCCEEDED",null,"a".repeat(64),100);ledger.succeed(id);
  assertThat(ledger.detail(id).run().getStatus()).isEqualTo("CANCELLED");assertThat(ledger.detail(id).steps().get(0).getOutputSha256()).isNull();
 }
 @Test void completedExecutionCannotBeCancelledAndAnotherOwnerCannotReadOrCancel()throws Exception{
  UUID id=begin();ledger.startStep(id,0);ledger.finishStep(id,0,"FAILED","DENIED",null,null);
  assertThatThrownBy(()->ledger.cancel(id)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
  when(permissions.current()).thenReturn(new PermissionSnapshot(202,true,Set.of()));
  assertThatThrownBy(()->ledger.detail(id)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
  assertThatThrownBy(()->ledger.cancel(id)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
 }
 @Test void summaryCountsRealOwnerScopedRunsAndPublishedWorkflows()throws Exception{
  UUID id=begin();assertThat(ledger.summary()).containsEntry("publishedWorkflows",1L).containsEntry("running7Days",1L).containsEntry("executions7Days",1L);
  ledger.cancel(id);assertThat(ledger.summary()).containsEntry("running7Days",0L);
  when(permissions.current()).thenReturn(new PermissionSnapshot(202,true,Set.of()));assertThat(ledger.summary()).containsEntry("publishedWorkflows",0L).containsEntry("executions7Days",0L);
 }
 @Test void executionFiltersApplyBeforePaginationAndStayOwnerScoped()throws Exception{
  UUID first=begin();ledger.cancel(first);UUID second=begin();
  assertThat(ledger.list(0,"Read product","CANCELLED",null,null,null).get("total")).isEqualTo(1L);
  assertThat(ledger.list(0,"Read product","RUNNING",null,null,null).get("total")).isEqualTo(1L);
  assertThat(ledger.list(0,"%","",null,null,null).get("total")).isEqualTo(0L);
  assertThat(ledger.list(0,"","",null,Instant.now().plusSeconds(60),null).get("total")).isEqualTo(0L);
  assertThat(ledger.list(0,"","",UUID.randomUUID(),null,null).get("total")).isEqualTo(0L);
  assertThatThrownBy(()->ledger.list(0,"","MADE_UP",null,null,null)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
  when(permissions.current()).thenReturn(new PermissionSnapshot(202,true,Set.of()));assertThat(ledger.list(0,"","",null,null,null).get("total")).isEqualTo(0L);
  assertThatThrownBy(()->ledger.logs(second,0)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
 }
 @Test void logTimelineComesFromPersistedAuditTransitions()throws Exception{
  UUID id=begin();ledger.startStep(id,0);ledger.finishStep(id,0,"SUCCEEDED",null,"a".repeat(64),120);ledger.cancel(id);
  var records=ledger.logs(id,0);assertThat(records.get("total")).isEqualTo(4L);
  @SuppressWarnings("unchecked") var logs=(List<WorkflowRunLedger.Log>)records.get("items");
  assertThat(logs).extracting(WorkflowRunLedger.Log::status).containsExactly("RUNNING","RUNNING","SUCCEEDED","CANCELLED");
  assertThat(logs.get(2).tool()).isEqualTo("PRODUCT_GET");assertThat(logs.get(2).stepKey()).isEqualTo("one");
  assertThat(logs).allSatisfy(log->assertThat(log.occurredAt()).isNotNull());
 }
}
