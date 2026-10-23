package com.example.eshop.ai.voice;

import com.example.eshop.ai.controller.AiController;
import com.example.eshop.ai.client.AuthToolClient;
import com.example.eshop.ai.service.*;
import com.example.eshop.ai.dto.*;
import com.example.eshop.ai.enums.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.Instant;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class LiveVoiceControllerTest {
    private final ObjectMapper mapper=new ObjectMapper();
    private final VoiceSessions sessions=mock(VoiceSessions.class);
    private final GeminiVoiceTokenService tokens=mock(GeminiVoiceTokenService.class);
    private final VoiceTools tools=new VoiceTools(mock(com.example.eshop.ai.registry.AiToolRegistry.class),mapper);
    private final com.example.eshop.common.security.LivePermissionService permissions=mock(com.example.eshop.common.security.LivePermissionService.class);
    private final AiChatStateService state=mock(AiChatStateService.class);
    private final AiController commerce=mock(AiController.class);
    private final AuthToolClient auth=mock(AuthToolClient.class);
    private final UUID id=UUID.randomUUID(),conversation=UUID.randomUUID();
    private final MockHttpServletRequest request=new MockHttpServletRequest();
    private final VoiceSessions.Session session=new VoiceSessions.Session(id,conversation,"km","CONNECTED",Instant.now().plusSeconds(300),"2026-10-23",300);
    private LiveVoiceController controller(boolean enabled){return new LiveVoiceController(new VoiceProperties(enabled,"gemini-3.1-flash-live-preview",true,false,true,5,300,1),tokens,sessions,tools,state,commerce,new AiChatPresenter(),auth,mapper,mock(AiVoiceAudit.class),permissions);}
    @BeforeEach void setup(){var actor=new UsernamePasswordAuthenticationToken("customer","",List.of());actor.setDetails(Map.of("userId",17L));SecurityContextHolder.getContext().setAuthentication(actor);when(sessions.active(id)).thenReturn(session);when(state.references(conversation)).thenReturn(mapper.createObjectNode());when(permissions.current()).thenReturn(new com.example.eshop.common.security.PermissionSnapshot(17,false,Set.of()));}
    @AfterEach void cleanup(){SecurityContextHolder.clearContext();}
    @Test void conflictsReturnSafeReasonAndActualLeaseRetryDelay(){
        when(sessions.retryAfterSeconds()).thenReturn(42L);
        var result=controller(true).voiceFailure(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"A voice call is already active."));
        assertThat(result.getStatusCode().value()).isEqualTo(409);
        assertThat(result.getHeaders().getFirst("Retry-After")).isEqualTo("42");
        assertThat(mapper.valueToTree(result.getBody()).path("retryAfterSeconds").asInt()).isEqualTo(42);
    }
    @Test void disabledIntegrationDoesNotReserveQuotaOrCallGoogle(){
        assertThatThrownBy(()->controller(false).create(new LiveVoiceController.Start(null,"km"),request)).hasMessageContaining("temporarily unavailable");
        verifyNoInteractions(sessions,tokens);
    }
    @Test void authenticationRequiredBeforeSessionCreation(){
        SecurityContextHolder.clearContext();assertThatThrownBy(()->controller(true).create(new LiveVoiceController.Start(null,"km"),request)).hasMessageContaining("authenticated");
        verifyNoInteractions(sessions,tokens);
    }
    @Test void arbitraryToolsAndModelSuppliedConfirmationAreRejected(){
        assertThatThrownBy(()->controller(true).tool(id,new LiveVoiceController.ToolCall("call","confirm_checkout",mapper.createObjectNode()),"Bearer customer",request)).hasMessageContaining("403");
        assertThatThrownBy(()->controller(true).tool(id,new LiveVoiceController.ToolCall("call","ROLE_GRANT",mapper.createObjectNode()),"Bearer customer",request)).hasMessageContaining("403");
        verifyNoInteractions(commerce);
    }
    @Test void commerceUsesExistingAuthorizedAndIdempotentControllerAndReturnsActualPendingStatus() throws Exception{
        var args=mapper.readTree("{\"orderId\":4}");
        var result=new AiResponse(UUID.randomUUID(),"request",null,AiIntent.PAYMENT_GET_STATUS,AiExecutionStatus.SUCCESS,"Current status",null,mapper.readTree("{\"orderId\":4,\"status\":\"PENDING\"}"));
        UUID key=UUID.nameUUIDFromBytes((id+":call-1").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        when(commerce.tool(AiIntent.PAYMENT_GET_STATUS,args,"Bearer customer",key,conversation,request)).thenReturn(ResponseEntity.ok(result));
        var response=controller(true).tool(id,new LiveVoiceController.ToolCall("call-1","get_payment_status",args),"Bearer customer",request);
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(mapper.valueToTree(response.getBody()).path("modelResult").path("data").path("status").asText()).isEqualTo("PENDING");
        verify(commerce).tool(AiIntent.PAYMENT_GET_STATUS,args,"Bearer customer",key,conversation,request);
    }
    @Test void customerCannotRequestAdminModeOrAdminTools(){
        when(tokens.configured()).thenReturn(true);
        assertThatThrownBy(()->controller(true).create(new LiveVoiceController.Start(null,"km",VoiceSessions.Mode.ADMIN),request)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(()->controller(true).tool(id,new LiveVoiceController.ToolCall("admin","admin_low_stock",mapper.createObjectNode()),"Bearer customer",request)).hasMessageContaining("403");
        verifyNoInteractions(commerce);verify(sessions,never()).reserve(any(),any(),any());
    }
    @Test void productSelectionUsesSameConversationAndRejectsForeignSku() throws Exception {
        var data=mapper.readTree("{\"id\":24,\"name\":\"Real product\",\"skus\":[{\"id\":89,\"available\":3}]}");
        var response=new AiResponse(null,"r",null,AiIntent.PRODUCT_DETAIL,AiExecutionStatus.SUCCESS,"Details",null,data);
        when(commerce.tool(eq(AiIntent.PRODUCT_DETAIL),any(),anyString(),any(),eq(conversation),eq(request))).thenReturn(ResponseEntity.ok(response));
        var selection=new LiveVoiceController.Selection(UUID.randomUUID(),24,99L);
        assertThatThrownBy(()->controller(true).selection(id,selection,"Bearer customer",request)).hasMessageContaining("does not belong");
        verify(commerce,never()).tool(eq(AiIntent.PRODUCT_STOCK_CHECK),any(),anyString(),any(),any(),any());
        var result=controller(true).selection(id,new LiveVoiceController.Selection(UUID.randomUUID(),24,null),"Bearer customer",request);
        var event=mapper.valueToTree(result.getBody()).path("event");assertThat(event.path("type").asText()).isEqualTo("PRODUCT_SELECTED");assertThat(event.path("conversationId").asText()).isEqualTo(conversation.toString());
        verifyNoInteractions(tokens);
    }
    private void adminSession(){
        when(sessions.active(id)).thenReturn(new VoiceSessions.Session(id,conversation,"km","CONNECTED",Instant.now().plusSeconds(300),"2026-10-23",300,VoiceSessions.Mode.ADMIN));
        when(permissions.current()).thenReturn(new com.example.eshop.common.security.PermissionSnapshot(17,true,Set.of("PRODUCT_VIEW","PAYMENT_VIEW_ALL","ORDER_VIEW_ALL","INVENTORY_VIEW","PROMOTION_VIEW")));
    }
    @Test void adminCanOpenWhitelistedScreensInsideSameCallWithoutMutations(){
        adminSession();
        for(var pair:Map.of("products","products.list","payments","payments.list","orders","orders.list","inventory","inventory.list","low_stock","inventory.low-stock","promotions","promotions.list").entrySet()){
            var result=controller(true).tool(id,new LiveVoiceController.ToolCall("show-"+pair.getKey(),"show_ui",mapper.createObjectNode().put("view",pair.getKey())),"Bearer admin",request);
            var body=mapper.valueToTree(result.getBody());assertThat(body.path("display").path("presentation").path("blocks").get(0).path("kind").asText()).isEqualTo(pair.getValue());assertThat(body.path("event").path("conversationId").asText()).isEqualTo(conversation.toString());assertThat(body.path("modelResult").path("status").asText()).isEqualTo("SUCCESS");
        }
        verify(permissions).require("PAYMENT_VIEW_ALL");verify(permissions).require("PRODUCT_VIEW");verifyNoInteractions(commerce,tokens);
    }
    @Test void uiRequestsCannotBypassCurrentPermissionsOrOpenArbitraryComponents(){
        assertThatThrownBy(()->controller(true).tool(id,new LiveVoiceController.ToolCall("show","show_ui",mapper.createObjectNode().put("view","products")),"Bearer customer",request)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        adminSession();doThrow(new org.springframework.security.access.AccessDeniedException("Missing payment permission")).when(permissions).require("PAYMENT_VIEW_ALL");
        assertThatThrownBy(()->controller(true).tool(id,new LiveVoiceController.ToolCall("show","show_ui",mapper.createObjectNode().put("view","payments")),"Bearer admin",request)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(()->controller(true).tool(id,new LiveVoiceController.ToolCall("show","show_ui",mapper.createObjectNode().put("view","arbitrary-component")),"Bearer admin",request)).hasMessageContaining("Unsupported screen");verifyNoInteractions(commerce,tokens);
    }
    @Test void showingCartUsesExistingAccountScopedReadBeforeOpeningCheckout(){
        adminSession();var result=new AiResponse(null,null,null,AiIntent.CART_GET,AiExecutionStatus.SUCCESS,"Own cart",null,mapper.createObjectNode().putArray("items"));
        when(commerce.tool(eq(AiIntent.CART_GET),any(),eq("Bearer admin"),any(),eq(conversation),eq(request))).thenReturn(ResponseEntity.ok(result));
        var response=controller(true).tool(id,new LiveVoiceController.ToolCall("cart","show_ui",mapper.createObjectNode().put("view","cart")),"Bearer admin",request);
        assertThat(mapper.valueToTree(response.getBody()).path("display").path("presentation").path("blocks").get(0).path("kind").asText()).isEqualTo("cart.checkout");verifyNoInteractions(tokens);
    }
    @Test void revokedAdministratorCannotContinueTools(){
        when(sessions.active(id)).thenReturn(new VoiceSessions.Session(id,conversation,"km","CONNECTED",Instant.now().plusSeconds(300),"2026-10-23",300,VoiceSessions.Mode.ADMIN));
        assertThatThrownBy(()->controller(true).tool(id,new LiveVoiceController.ToolCall("admin","admin_low_stock",mapper.createObjectNode()),"Bearer customer",request)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);verifyNoInteractions(commerce);
    }
    @Test void savedAddressLookupIgnoresModelUserIdAndReturnsOnlyReferences() throws Exception{
        when(auth.addresses("Bearer customer",17)).thenReturn(mapper.readTree("[{\"data\":{\"id\":9,\"is_default\":true,\"phone_number\":\"private\",\"address_line1\":\"private\"}}]"));
        var response=controller(true).tool(id,new LiveVoiceController.ToolCall("addresses","get_saved_addresses",mapper.readTree("{\"userId\":99}")),"Bearer customer",request);
        assertThat(mapper.writeValueAsString(response.getBody())).contains("is_default","9").doesNotContain("private","phone","address_line1");
        verify(auth).addresses("Bearer customer",17);
    }
}
