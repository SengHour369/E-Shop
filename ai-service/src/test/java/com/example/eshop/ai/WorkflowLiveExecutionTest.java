package com.example.eshop.ai;

import com.example.eshop.ai.workflow.*;
import com.example.eshop.ai.registry.AiToolRegistry;
import com.example.eshop.ai.service.AiToolExecutor;
import com.example.eshop.ai.client.CatalogToolClient;
import com.example.eshop.common.security.*;
import com.example.eshop.common.audit.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.transaction.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import feign.Feign;
import feign.Retryer;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import java.util.*;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

/** Opt-in REAL catalog HTTP calls. No mocked executor, business clients, responses or audit service.
 * Only identity is a test fixture; gateway/authentication is tested separately, never claimed here.
 */
@EnabledIfEnvironmentVariable(named="WORKFLOW_LIVE_CATALOG_URL",matches="http://(?:localhost|127\\.0\\.0\\.1):[0-9]+")
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest(properties={"spring.jpa.hibernate.ddl-auto=create-drop","spring.datasource.url=jdbc:h2:mem:workflow_live;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password="})
@Transactional(propagation=Propagation.NOT_SUPPORTED)
@Import({WorkflowController.class,WorkflowPublicationService.class,WorkflowManualRunner.class,WorkflowRunLedger.class,AiToolRegistry.class,ObjectMapper.class,AuditLogService.class,AuditContextProvider.class,AuditSnapshots.class,WorkflowLiveExecutionTest.Wiring.class})
class WorkflowLiveExecutionTest {
 @TestConfiguration static class Wiring {
  @Bean LivePermissionService identityFixture(){return new LivePermissionService("http://unused.invalid"){@Override public PermissionSnapshot current(){return new PermissionSnapshot(CurrentActor.userId(),true,Set.of());}};}
  @Bean AiToolExecutor executor(AiToolRegistry registry,ObjectMapper mapper){
   var catalog=Feign.builder().contract(new SpringMvcContract()).retryer(Retryer.NEVER_RETRY)
    .options(new feign.Request.Options(2000,5000))
    .decoder((response,type)->mapper.readTree(response.body().asInputStream()))
    .target(CatalogToolClient.class,System.getenv("WORKFLOW_LIVE_CATALOG_URL"));
   return new AiToolExecutor(catalog,null,null,null,registry,null,null,null,null,null,null,mapper);
  }
 }
 @Autowired WorkflowController drafts;
 @Autowired WorkflowPublicationService publications;
 @Autowired WorkflowManualRunner runner;
 @Autowired WorkflowRunLedger ledger;
 @Autowired WorkflowRunRepository runs;
 @Autowired WorkflowPublishedRepository versions;
 @Autowired WorkflowDraftRepository draftRows;
 @Autowired WorkflowRunStepRepository steps;
 @Autowired AuditLogRepository audit;
 @Autowired ObjectMapper mapper;
 @BeforeEach void actor(){var authentication=new UsernamePasswordAuthenticationToken("workflow-test",null,List.of());authentication.setDetails(Map.of("userId",101L));SecurityContextHolder.getContext().setAuthentication(authentication);}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 WorkflowController.Draft draft(long firstId,long secondId)throws Exception{
  var definition=mapper.createObjectNode();definition.put("name","Live catalog read "+UUID.randomUUID());definition.put("description","Real catalog HTTP checks; read-only");definition.put("module","catalog-service");definition.put("triggerType","MANUAL");var items=definition.putArray("steps");int i=0;for(long id:new long[]{firstId,secondId}){var step=items.addObject();step.put("key","read-"+i++);step.put("name","Read catalog product");step.put("tool","PRODUCT_GET");step.putObject("parameters").put("id",id);}return drafts.create(new WorkflowController.Write(null,definition));
 }
 long product(){return Long.parseLong(System.getenv().getOrDefault("WORKFLOW_LIVE_PRODUCT_ID","1"));}
 @Test void executesTwoRealHttpStepsAndPersistsEvidenceWithoutPayloads()throws Exception{
  var draft=draft(product(),product());var version=publications.publish(draft.id(),draft.revision());UUID key=UUID.randomUUID();long before=audit.count((root,query,builder)->builder.conjunction());var result=runner.execute(version.getId(),key,null);
  assertThat(result.run().getStatus()).isEqualTo("SUCCEEDED");assertThat(result.steps()).hasSize(2).allSatisfy(s->{assertThat(s.getStatus()).isEqualTo("SUCCEEDED");assertThat(s.getOutputSha256()).hasSize(64);assertThat(s.getOutputBytes()).isPositive();assertThat(s.getStartedAt()).isNotNull();assertThat(s.getFinishedAt()).isNotNull();});assertThat(audit.count((root,query,builder)->builder.conjunction())).isGreaterThan(before);
  var repeat=runner.execute(version.getId(),key,null);assertThat(repeat.run().getFinishedAt()).isEqualTo(result.run().getFinishedAt());assertThat(repeat.steps()).extracting(WorkflowRunStep::getId).containsExactlyElementsOf(result.steps().stream().map(WorkflowRunStep::getId).toList());assertThat(audit.count((root,query,builder)->builder.conjunction())).isEqualTo(before+6);
 }
 @Test void upstreamMissingProductNeverBecomesSuccessAndStopsLaterSteps()throws Exception{
  var draft=draft(Long.MAX_VALUE,product());var version=publications.publish(draft.id(),draft.revision());var result=runner.execute(version.getId(),UUID.randomUUID(),null);assertThat(result.run().getStatus()).isEqualTo("UNKNOWN");assertThat(result.steps().get(0).getStatus()).isEqualTo("UNKNOWN");assertThat(result.steps().get(0).getOutputSha256()).isNull();assertThat(result.steps().get(1).getStatus()).isEqualTo("SKIPPED");
 }
 @Test void publishedSnapshotSurvivesDraftChangesAndRejectsStalePublication()throws Exception{
  var draft=draft(product(),product());var published=publications.publish(draft.id(),draft.revision());assertThat(publications.publish(draft.id(),draft.revision()).getId()).isEqualTo(published.getId());var change=draft.definition().deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)change).put("name","Updated editable draft");drafts.update(draft.id(),new WorkflowController.Write(draft.revision(),change));assertThat(versions.findById(published.getId()).orElseThrow().getDefinition()).doesNotContain("Updated editable draft");assertThatThrownBy(()->publications.publish(draft.id(),draft.revision())).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
 }
 @Test void rejectsWriteToolPublicationAndCrossOwnerExecution()throws Exception{
  var draft=draft(product(),product());var changed=draft.definition().deepCopy();var step=(com.fasterxml.jackson.databind.node.ObjectNode)changed.path("steps").get(0);step.put("tool","CART_ADD");step.putObject("parameters").put("skuId",1).put("quantity",1);var updated=drafts.update(draft.id(),new WorkflowController.Write(draft.revision(),changed));assertThatThrownBy(()->publications.publish(draft.id(),updated.revision())).isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
  var valid=draft(product(),product());var version=publications.publish(valid.id(),valid.revision());var auth=new UsernamePasswordAuthenticationToken("other",null,List.of());auth.setDetails(Map.of("userId",202L));SecurityContextHolder.getContext().setAuthentication(auth);assertThatThrownBy(()->runner.execute(version.getId(),UUID.randomUUID(),null)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
 }
 @Test void rejectsKeyReuseAcrossVersionsAndReconcilesStaleRunsWithoutReplay()throws Exception{
  var first=draft(product(),product());var version=publications.publish(first.id(),first.revision());var second=draft(product(),product());var other=publications.publish(second.id(),second.revision());UUID key=UUID.randomUUID();ledger.begin(version.getId(),key);assertThatThrownBy(()->runner.execute(other.getId(),key,null)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");assertThatThrownBy(()->ledger.reconcile(key)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
  ledger.startStep(key,0);var stale=runs.findById(key).orElseThrow();stale.setStartedAt(Instant.now().minusSeconds(1000));runs.saveAndFlush(stale);var result=ledger.reconcile(key);assertThat(result.run().getStatus()).isEqualTo("UNKNOWN");assertThat(result.steps()).extracting(WorkflowRunStep::getStatus).containsExactly("UNKNOWN","SKIPPED");assertThat(runner.execute(version.getId(),key,null).run().getStatus()).isEqualTo("UNKNOWN");
 }
 @Test void conditionBranchesOnRealHttpResponseAndRecordsUnselectedSteps()throws Exception{
  var original=draft(product(),product());var definition=(com.fasterxml.jackson.databind.node.ObjectNode)original.definition().deepCopy();var items=(com.fasterxml.jackson.databind.node.ArrayNode)definition.path("steps");items.remove(1);
  var condition=items.addObject();condition.put("key","decision");condition.put("name","Inspect real response");condition.put("tool","WORKFLOW_CONDITION");condition.putObject("parameters").put("sourceStep","read-0").put("path","").put("operator","EXISTS").put("trueNext","yes-end").put("falseNext","no-read");
  var no=items.addObject();no.put("key","no-read");no.put("name","Not selected product read");no.put("tool","PRODUCT_GET");no.putObject("parameters").put("id",Long.MAX_VALUE);no.put("next","$end");
  var noEnd=items.addObject();noEnd.put("key","no-end");noEnd.put("name","No end");noEnd.put("tool","WORKFLOW_END");noEnd.putObject("parameters");
  var yes=items.addObject();yes.put("key","yes-end");yes.put("name","Yes end");yes.put("tool","WORKFLOW_END");yes.putObject("parameters");
  var saved=drafts.update(original.id(),new WorkflowController.Write(original.revision(),definition));var version=publications.publish(saved.id(),saved.revision());var result=runner.execute(version.getId(),UUID.randomUUID(),null);
  assertThat(result.run().getStatus()).isEqualTo("SUCCEEDED");assertThat(result.steps()).extracting(WorkflowRunStep::getStatus).containsExactly("SUCCEEDED","SUCCEEDED","SKIPPED","SKIPPED","SUCCEEDED");assertThat(result.steps().get(2).getErrorCode()).isEqualTo("BRANCH_NOT_SELECTED");
  var parameters=(com.fasterxml.jackson.databind.node.ObjectNode)condition.path("parameters");parameters.put("operator","EQ").put("value",false);((com.fasterxml.jackson.databind.node.ObjectNode)no.path("parameters")).put("id",product());
  var updated=drafts.update(saved.id(),new WorkflowController.Write(saved.revision(),definition));var otherVersion=publications.publish(updated.id(),updated.revision());var other=runner.execute(otherVersion.getId(),UUID.randomUUID(),null);
  assertThat(other.run().getStatus()).isEqualTo("SUCCEEDED");assertThat(other.steps()).extracting(WorkflowRunStep::getStatus).containsExactly("SUCCEEDED","SUCCEEDED","SUCCEEDED","SKIPPED","SKIPPED");

 }
 @Test void boundedRepeatCallsRealCatalogAndDelayActuallyWaits()throws Exception{
  var original=draft(product(),product());var definition=(com.fasterxml.jackson.databind.node.ObjectNode)original.definition().deepCopy();var items=(com.fasterxml.jackson.databind.node.ArrayNode)definition.path("steps");items.removeAll();
  var repeated=items.addObject();repeated.put("key","repeat");repeated.put("name","Read product twice");repeated.put("tool","WORKFLOW_REPEAT");var parameters=repeated.putObject("parameters");parameters.put("tool","PRODUCT_GET").put("count",2);parameters.putObject("parameters").put("id",product());
  var delay=items.addObject();delay.put("key","wait");delay.put("name","Wait one second");delay.put("tool","WORKFLOW_DELAY");delay.putObject("parameters").put("seconds",1);
  var end=items.addObject();end.put("key","end");end.put("name","End");end.put("tool","WORKFLOW_END");end.putObject("parameters");
  var saved=drafts.update(original.id(),new WorkflowController.Write(original.revision(),definition));var version=publications.publish(saved.id(),saved.revision());var key=UUID.randomUUID();var result=runner.execute(version.getId(),key,null);
  assertThat(result.run().getStatus()).isEqualTo("SUCCEEDED");assertThat(result.steps()).extracting(WorkflowRunStep::getStatus).containsExactly("SUCCEEDED","SUCCEEDED","SUCCEEDED");assertThat(java.time.Duration.between(result.steps().get(1).getStartedAt(),result.steps().get(1).getFinishedAt()).toMillis()).isGreaterThanOrEqualTo(1000);
  @SuppressWarnings("unchecked") var logs=(List<WorkflowRunLedger.Log>)ledger.logs(key,0).get("items");assertThat(logs).extracting(WorkflowRunLedger.Log::status).contains("READ_ITERATION_1","READ_ITERATION_2");assertThat(logs.stream().filter(log->log.status().startsWith("READ_ITERATION_")).toList()).allSatisfy(log->assertThat(log.errorCode()).isNull());
 }
}
