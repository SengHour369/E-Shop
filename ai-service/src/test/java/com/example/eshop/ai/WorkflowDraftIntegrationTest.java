package com.example.eshop.ai;

import com.example.eshop.ai.workflow.*;
import com.example.eshop.ai.registry.AiToolRegistry;
import com.example.eshop.common.security.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

// Real H2 persistence and real registry/schema validation; identity grants are a test fixture.
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace=org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest(properties={"spring.jpa.hibernate.ddl-auto=create-drop","spring.datasource.url=jdbc:h2:mem:workflow_drafts;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password="})
@Import({WorkflowController.class,AiToolRegistry.class,ObjectMapper.class})
class WorkflowDraftIntegrationTest {
 @Autowired WorkflowController controller;
 @Autowired WorkflowDraftRepository drafts;
 @Autowired ObjectMapper mapper;
 @MockitoBean LivePermissionService permissions;
 @BeforeEach void setup(){when(permissions.current()).thenReturn(new PermissionSnapshot(1,true,Set.of()));SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin",null,List.of()));}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 WorkflowController.Write write(Long revision,String name)throws Exception{return new WorkflowController.Write(revision,mapper.readTree("{\"name\":\""+name+"\",\"description\":\"Catalog review\",\"triggerType\":\"MANUAL\",\"module\":\"catalog-service\",\"steps\":[{\"key\":\"one\",\"name\":\"Read product\",\"tool\":\"PRODUCT_GET\",\"parameters\":{\"id\":1}}]}"));}
 @Test void persistsAndUpdatesWithRevision()throws Exception{var created=controller.create(write(null,"Catalog review"));assertThat(drafts.findById(created.id())).isPresent();var saved=controller.update(created.id(),write(created.revision(),"Updated review"));assertThat(saved.revision()).isGreaterThan(created.revision());assertThat(controller.list(0,"Updated").get("total")).isEqualTo(1L);assertThatThrownBy(()->controller.update(created.id(),write(created.revision(),"Stale revision"))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");}
 @Test void validatesRealRegistryWithoutExecutingTools()throws Exception{var result=controller.validate(write(null,"Catalog review"));assertThat(result.get("valid")).isEqualTo(true);assertThat(result.get("executed")).isEqualTo(false);assertThat(drafts.count()).isZero();}
 @Test void rejectsUnknownToolsAndArguments()throws Exception{var body=write(null,"Catalog review");var invalid=mapper.readTree(body.definition().toString().replace("PRODUCT_GET","ARBITRARY_HTTP"));assertThat(controller.validate(new WorkflowController.Write(null,invalid)).get("valid")).isEqualTo(false);var extra=mapper.readTree(body.definition().toString().replace("\"id\":1","\"id\":1,\"url\":\"https://example.org\""));assertThat(controller.validate(new WorkflowController.Write(null,extra)).get("valid")).isEqualTo(false);}
 @Test void deniesNonAdminAndCrossOwnerReads()throws Exception{var saved=controller.create(write(null,"Private draft"));when(permissions.current()).thenReturn(new PermissionSnapshot(2,true,Set.of()));assertThatThrownBy(()->controller.get(saved.id())).isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");when(permissions.current()).thenReturn(new PermissionSnapshot(2,false,Set.of()));assertThatThrownBy(()->controller.catalog()).isInstanceOf(AccessDeniedException.class);}
 @Test void rejectsCredentials()throws Exception{var body=mapper.readTree("{\"name\":\"Unsafe draft\",\"steps\":[],\"token\":\"must-not-store\"}");assertThatThrownBy(()->controller.create(new WorkflowController.Write(null,body))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("credentials");}
 @Test void conditionGraphRejectsCyclesAndUnknownTargets()throws Exception{
  var body=write(null,"Branch workflow");var definition=(com.fasterxml.jackson.databind.node.ObjectNode)body.definition().deepCopy();var condition=((com.fasterxml.jackson.databind.node.ArrayNode)definition.path("steps")).addObject();condition.put("key","decision");condition.put("name","Condition");condition.put("tool","WORKFLOW_CONDITION");var parameters=condition.putObject("parameters");parameters.put("sourceStep","one").put("path","").put("operator","EXISTS").put("trueNext","one").put("falseNext","$end");assertThat(controller.validate(new WorkflowController.Write(null,definition)).get("valid")).isEqualTo(false);parameters.put("trueNext","missing");assertThat(controller.validate(new WorkflowController.Write(null,definition)).get("valid")).isEqualTo(false);parameters.put("trueNext","$end");assertThat(controller.validate(new WorkflowController.Write(null,definition)).get("valid")).isEqualTo(true);
 }
 @Test void delayBudgetsAndRepeatWriteTargetsAreRejected()throws Exception{
  var body=write(null,"Bounded controls");var definition=(com.fasterxml.jackson.databind.node.ObjectNode)body.definition().deepCopy();var items=(com.fasterxml.jackson.databind.node.ArrayNode)definition.path("steps");items.removeAll();
  for(int i=0;i<2;i++){var delay=items.addObject();delay.put("key","delay-"+i);delay.put("name","Wait");delay.put("tool","WORKFLOW_DELAY");delay.putObject("parameters").put("seconds",20);}assertThat(controller.validate(new WorkflowController.Write(null,definition)).get("valid")).isEqualTo(false);
  items.removeAll();var repeated=items.addObject();repeated.put("key","repeat");repeated.put("name","Do not repeat writes");repeated.put("tool","WORKFLOW_REPEAT");var parameters=repeated.putObject("parameters");parameters.put("tool","ORDER_CANCEL").put("count",2);parameters.putObject("parameters").put("orderNumber","irrelevant");assertThat(controller.validate(new WorkflowController.Write(null,definition)).get("valid")).isEqualTo(false);
  parameters.put("tool","PRODUCT_GET").put("count",11);parameters.putObject("parameters").put("id",1);assertThat(controller.validate(new WorkflowController.Write(null,definition)).get("valid")).isEqualTo(false);parameters.put("count",2);assertThat(controller.validate(new WorkflowController.Write(null,definition)).get("valid")).isEqualTo(true);
 }
}
