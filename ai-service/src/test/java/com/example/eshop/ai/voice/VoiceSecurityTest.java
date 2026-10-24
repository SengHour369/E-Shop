package com.example.eshop.ai.voice;

import com.example.eshop.ai.registry.*;
import com.example.eshop.common.security.CurrentActor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Validation;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.core.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class VoiceSecurityTest {
    private final ObjectMapper mapper=new ObjectMapper().registerModule(new JavaTimeModule());
    private VoiceProperties properties(boolean enabled,boolean verified,String model){return new VoiceProperties(enabled,model,true,false,verified,5,300,1);}
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    private void actor(long id){var auth=new UsernamePasswordAuthenticationToken("customer","",List.of());auth.setDetails(Map.of("userId",id));SecurityContextHolder.getContext().setAuthentication(auth);}
    @Test void freeOnlyRequiresExplicitVerifiedProjectAndApprovedModel(){
        assertThat(properties(true,true,"gemini-3.1-flash-live-preview").allowed()).isTrue();
        assertThat(properties(true,false,"gemini-3.1-flash-live-preview").allowed()).isFalse();
        assertThat(properties(false,true,"gemini-3.1-flash-live-preview").allowed()).isFalse();
        assertThat(properties(true,true,"paid-model").allowed()).isFalse();
        try(var factory=Validation.buildDefaultValidatorFactory()){
            assertThat(factory.getValidator().validate(new VoiceProperties(true,"x",false,true,true,6,301,2))).hasSize(5);
        }
    }
    @Test void exportsActualCustomerToolContractForOptionalProviderSmokeTest() throws Exception {
        actor(17);
        var permissions=mock(com.example.eshop.common.security.LivePermissionService.class);
        when(permissions.current()).thenReturn(new com.example.eshop.common.security.PermissionSnapshot(17,false,Set.of()));
        var declarations=new VoiceTools(new AiToolRegistry(permissions),mapper).declarations();
        assertThat(declarations).hasSize(18);
        assertThat(declarations.stream().map(d->d.get("name"))).contains("get_cart","search_products","prepare_checkout").doesNotContain("confirm_checkout");
        String path=System.getProperty("voice.fixture.path");
        if(path!=null)java.nio.file.Files.writeString(java.nio.file.Path.of(path),mapper.writeValueAsString(declarations));
    }
    @Test void adminDeclarationsRequireCurrentSpecificPermissionsAndCustomerModeNeverExportsThem() throws Exception {
        actor(17);var permissions=mock(com.example.eshop.common.security.LivePermissionService.class);
        when(permissions.current()).thenReturn(new com.example.eshop.common.security.PermissionSnapshot(17,true,Set.of("INVENTORY_VIEW","PROMOTION_VIEW","ORDER_VIEW")));
        var tools=new VoiceTools(new AiToolRegistry(permissions),mapper);
        assertThat(tools.declarations(VoiceSessions.Mode.CUSTOMER).stream().map(d->d.get("name"))).doesNotContain("admin_low_stock","admin_list_orders","admin_list_promotions","show_ui");
        var admin=tools.declarations(VoiceSessions.Mode.ADMIN);
        assertThat(admin.stream().map(d->d.get("name"))).contains("admin_low_stock","admin_list_promotions","show_ui").doesNotContain("disable_promotion","prepare_promotion","stage_import_draft");
        String path=System.getProperty("voice.admin.fixture.path");if(path!=null)java.nio.file.Files.writeString(java.nio.file.Path.of(path),mapper.writeValueAsString(admin));
        when(permissions.current()).thenReturn(new com.example.eshop.common.security.PermissionSnapshot(17,false,Set.of()));
        assertThat(tools.declarations(VoiceSessions.Mode.ADMIN).stream().map(d->d.get("name"))).doesNotContain("admin_low_stock","admin_list_orders","admin_list_promotions");
    }
    @Test void actualProductSpecificationsAndAdminStockSurviveMinimization() throws Exception {
        var tools=new VoiceTools(mock(AiToolRegistry.class),mapper);
        var actual=mapper.readTree("{\"id\":1,\"description\":\"Stored features\",\"brand\":\"Stored brand\",\"skus\":[{\"id\":42,\"sku\":\"BLACK-42\",\"effectivePrice\":80,\"promotionEndAt\":\"2026-10-24T12:00:00\",\"attributes\":[{\"name\":\"Color\",\"attributes\":[{\"value\":\"Black\"}]}]}],\"available_qty\":3,\"product_id\":1,\"phone\":\"secret\"}");
        var result=tools.minimize(actual);assertThat(result.path("skus").get(0).path("attributes").get(0).path("attributes").get(0).path("value").asText()).isEqualTo("Black");assertThat(result.path("available_qty").asInt()).isEqualTo(3);
        assertThat(tools.minimize(mapper.readTree("{\"currency\":\"USD\",\"paymentCount\":7,\"total\":80,\"confirmed\":4}")).path("paymentCount").asInt()).isEqualTo(7);assertThat(result.toString()).doesNotContain("secret");
    }
    @Test void midnightUsesCambodianBusinessDay(){
        Instant now=Instant.parse("2026-10-23T17:00:00Z");
        assertThat(VoiceSessions.day(now)).isEqualTo("2026-10-24");
        assertThat(VoiceSessions.reset(now)).isEqualTo(Instant.parse("2026-10-24T17:00:00Z").getEpochSecond());
    }
    @Test void modelCannotInvokeConfirmationOrAdministrativeTools(){
        assertThat(VoiceTools.ALLOWED).doesNotContainKeys("confirm_checkout","confirm","ROLE_GRANT","PAYMENT_REFUND","ADMIN_IMPORT_DRAFT");
    }
    @Test void minimizeRemovesPersonalAndPaymentSecretsRecursively() throws Exception{
        var tools=new VoiceTools(mock(AiToolRegistry.class),mapper);
        var result=tools.minimize(mapper.readTree("""
            {"id":1,"price":10,"currency":"USD","status":"PENDING","phone_number":"secret",
             "address_line1":"private","qr":"private","token":"secret","items":[{"skuId":2,"quantity":1,"recipient_name":"private"}]}
            """));
        assertThat(result.toString()).doesNotContain("private","secret","phone","address_line1","token","qr");
        assertThat(result.path("items").get(0).path("skuId").asInt()).isEqualTo(2);
        assertThat(result.path("status").asText()).isEqualTo("PENDING");
    }
    @Test void storageFailuresFailClosedAndIdentityCannotComeFromClient(){
        var redis=mock(StringRedisTemplate.class);var sessions=new VoiceSessions(redis,mapper,properties(true,true,"gemini-3.1-flash-live-preview"));
        assertThatThrownBy(sessions::usage).isInstanceOf(ResponseStatusException.class);
        actor(12);when(redis.opsForValue()).thenThrow(new IllegalStateException("offline"));
        assertThatThrownBy(sessions::usage).isInstanceOf(ResponseStatusException.class).hasMessageContaining("503");
    }
    @Test void readsOnlyAuthenticatedUsersSessionNamespaceAndRejectsEndedOrExpired() throws Exception{
        actor(12);var redis=mock(StringRedisTemplate.class);ValueOperations<String,String> values=mock(ValueOperations.class);when(redis.opsForValue()).thenReturn(values);
        var sessions=new VoiceSessions(redis,mapper,properties(true,true,"gemini-3.1-flash-live-preview"));UUID id=UUID.randomUUID();
        assertThatThrownBy(()->sessions.owned(id)).hasMessageContaining("410");
        verify(values).get("ai:voice:{12}:session:"+id);
        var ended=new VoiceSessions.Session(id,UUID.randomUUID(),"km","ENDED",Instant.now().plusSeconds(30),"2026-10-23",300);
        when(values.get(anyString())).thenReturn(mapper.writeValueAsString(ended));
        assertThatThrownBy(()->sessions.active(id)).hasMessageContaining("410");
        var expired=new VoiceSessions.Session(id,UUID.randomUUID(),"km","CREATED",Instant.now().minusSeconds(1),"2026-10-23",300);
        when(values.get(anyString())).thenReturn(mapper.writeValueAsString(expired));
        assertThatThrownBy(()->sessions.owned(id)).hasMessageContaining("410");
    }
}
