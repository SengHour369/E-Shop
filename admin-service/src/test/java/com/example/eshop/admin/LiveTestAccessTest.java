package com.example.eshop.admin;
import com.example.eshop.admin.model.LiveTestRun;
import com.example.eshop.admin.repository.LiveTestRunRepository;
import com.example.eshop.common.security.LivePermissionService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import java.time.Instant;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:live-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=create-drop","eureka.client.enabled=false","spring.cloud.discovery.enabled=false","admin.monitor-enabled=false","jwt.secret=66546A5744444446E5A7234743777217A25432A462D4A614E645267556B587032733576"})
@AutoConfigureMockMvc
class LiveTestAccessTest {
 @Autowired MockMvc mvc;
 @Autowired LiveTestRunRepository runs;
 @Autowired com.example.eshop.admin.livetest.LiveTestService service;
 @Autowired com.example.eshop.common.audit.AuditLogRepository audits;
 @MockitoBean LivePermissionService permissions;
 private UsernamePasswordAuthenticationToken actor(long id){var token=new UsernamePasswordAuthenticationToken("actor",null,List.of(new SimpleGrantedAuthority("ADMIN")));token.setDetails(Map.of("userId",id));return token;}
 @Test void anonymousCannotReadOrStartRealRuns() throws Exception {
  mvc.perform(get("/api/v1/admin/live-tests/runs")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/v1/admin/live-tests/runs").contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
 }
 @Test void reportOwnershipIsEnforcedByStoredActor() throws Exception {
  var run=new LiveTestRun();run.setId(UUID.randomUUID());run.setActorId(41);run.setStatus("PASSED");run.setStartedAt(Instant.now());run.setFinishedAt(Instant.now());run.setStepsJson("[]");runs.saveAndFlush(run);
  mvc.perform(get("/api/v1/admin/live-tests/runs/"+run.getId()).with(authentication(actor(42)))).andExpect(status().isNotFound());
  mvc.perform(get("/api/v1/admin/live-tests/runs/"+run.getId()).with(authentication(actor(41)))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PASSED"));
 }
 @Test @org.springframework.transaction.annotation.Transactional
 void startAndAuditAreAtomicAndRequestsWaitForCommit(){
  var report=service.start(41,"Bearer unit-test-not-sent","http://localhost:4200",false);
  org.junit.jupiter.api.Assertions.assertTrue(runs.existsById(report.id()));
  org.junit.jupiter.api.Assertions.assertTrue(audits.findAll((root,query,builder)->builder.equal(root.get("resourceId"),report.id().toString())).stream().anyMatch(a->"LIVE_TEST_RUN".equals(a.getResourceType())&&report.id().toString().equals(a.getResourceId())));
  org.junit.jupiter.api.Assertions.assertTrue(report.steps().stream().allMatch(s->"PENDING".equals(s.status())));
 }
 @Test void missingPermissionRejectsRequests() throws Exception {
  doThrow(new AccessDeniedException("Missing permission")).when(permissions).require("SYSTEM_TEST_VIEW");
  mvc.perform(get("/api/v1/admin/live-tests/runs").with(authentication(actor(41)))).andExpect(status().isForbidden());
 }
}
