package com.example.eshop.ai.voice;

import com.example.eshop.ai.client.AdminToolClient;
import com.example.eshop.ai.enums.AiIntent;
import com.example.eshop.ai.registry.AiToolRegistry;
import com.example.eshop.ai.service.AiToolExecutor;
import com.example.eshop.common.security.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminAiServiceAccessTest {
 final ObjectMapper mapper=new ObjectMapper();
 final LivePermissionService permissions=mock(LivePermissionService.class);
 final AiToolRegistry registry=new AiToolRegistry(permissions);
 final AdminToolClient admin=mock(AdminToolClient.class);
 @BeforeEach void setup(){
  var auth=new UsernamePasswordAuthenticationToken("admin-test","",List.of());auth.setDetails(Map.of("userId",17L));SecurityContextHolder.getContext().setAuthentication(auth);
  var grants=Arrays.stream(AiIntent.values()).map(registry::find).filter(Objects::nonNull).map(tool->tool.requiredPermission()).collect(Collectors.toSet());
  when(permissions.current()).thenReturn(new PermissionSnapshot(17,true,grants));
 }
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 AiToolExecutor executor(){return new AiToolExecutor(null,null,null,null,registry,null,null,null,null,null,null,mapper,admin);}
 @Test void adminVoiceExportsEveryEnabledPermittedRegisteredOperationWithoutDuplicateNames(){
  var tools=new VoiceTools(registry,mapper);var declarations=tools.declarations(VoiceSessions.Mode.ADMIN);
  var resolved=declarations.stream().map(row->tools.intent((String)row.get("name"),VoiceSessions.Mode.ADMIN)).filter(Objects::nonNull).collect(Collectors.toSet());
  assertThat(resolved).containsAll(registry.available().stream().map(tool->tool.toolName()).toList()).doesNotContain(AiIntent.ROLE_GRANT,AiIntent.PAYMENT_REFUND);
  assertThat(declarations.stream().map(row->row.get("name")).toList()).doesNotHaveDuplicates();
  assertThat(registry.available().stream().map(tool->tool.service()).collect(Collectors.toSet())).contains("catalog-service","order-service","payment-service","auth-service","notification-service","admin-service","ai-service");
  assertThat(tools.intent("admin_tool_list",VoiceSessions.Mode.ADMIN)).isEqualTo(AiIntent.ADMIN_TOOL_LIST);
  assertThat(tools.intent("admin_user_list",VoiceSessions.Mode.CUSTOMER)).isNull();
  assertThat(tools.intent("admin_role_grant",VoiceSessions.Mode.ADMIN)).isNull();
  assertThat(tools.intent("admin_sql",VoiceSessions.Mode.ADMIN)).isNull();
 }
 @Test void permissionRevocationRemovesAdminOperationsAndCustomersCannotListCapabilities(){
  var tools=new VoiceTools(registry,mapper);when(permissions.current()).thenReturn(new PermissionSnapshot(17,false,Set.of("USER_VIEW","AUDIT_VIEW")));
  assertThat(registry.available().stream().map(tool->tool.toolName()).toList()).doesNotContain(AiIntent.ADMIN_TOOL_LIST,AiIntent.ADMIN_USER_LIST,AiIntent.ADMIN_AUDIT_LOG,AiIntent.ADMIN_SERVICE_STATUS);
  assertThatThrownBy(()->registry.authorize(AiIntent.ADMIN_TOOL_LIST)).hasMessageContaining("cannot perform");
  assertThat(tools.declarations().stream().map(row->row.get("name")).toList()).doesNotContain("admin_tool_list","admin_user_list","admin_service_status");
 }
 @Test void capabilityDiscoveryListsFixedEndpointsArgumentsAndConfirmationRequirements(){
  var result=executor().execute(registry.find(AiIntent.ADMIN_TOOL_LIST),mapper.createObjectNode(),"Bearer admin");
  assertThat(result.isArray()).isTrue();
  var writes=new ArrayList<com.fasterxml.jackson.databind.JsonNode>();result.forEach(row->{if(row.path("toolName").asText().equals("CART_ADD"))writes.add(row);});
  assertThat(writes).hasSize(1);assertThat(writes.get(0).path("confirmationRequired").asBoolean()).isTrue();assertThat(writes.get(0).path("parameters").isArray()).isTrue();
  var minimized=new VoiceTools(registry,mapper).minimize(writes.get(0));assertThat(minimized.path("parameters").get(0).has("name")).isTrue();assertThat(minimized.path("parameters").get(0).has("type")).isTrue();assertThat(minimized.path("confirmationRequired").asBoolean()).isTrue();
  assertThat(result.toString()).contains("ADMIN_USER_LIST","ADMIN_AUTOMATION_REVIEW").doesNotContain("ROLE_GRANT","PAYMENT_REFUND","jdbc:");
 }
 @Test void adminServiceReadsForwardJwtAndReturnActualMonitorAndFeatureFlags() throws Exception {
  when(admin.monitor("Bearer admin")).thenReturn(mapper.readTree("{\"data\":{\"healthy\":true,\"outcome\":\"SUCCESS\"}}"));
  var monitor=executor().execute(registry.find(AiIntent.ADMIN_SERVICE_STATUS),mapper.createObjectNode(),"Bearer admin");assertThat(monitor.path("healthy").asBoolean()).isTrue();
  when(admin.storefrontAccess("Bearer admin")).thenReturn(mapper.readTree("{\"guestCart\":false}"));
  var access=executor().execute(registry.find(AiIntent.ADMIN_STOREFRONT_ACCESS),mapper.createObjectNode(),"Bearer admin");assertThat(access.path("guestCart").asBoolean()).isFalse();verify(admin).monitor("Bearer admin");verify(admin).storefrontAccess("Bearer admin");
 }
}
