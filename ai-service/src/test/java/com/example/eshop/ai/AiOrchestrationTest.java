package com.example.eshop.ai;
import com.example.eshop.ai.client.*;
import com.example.eshop.ai.dto.*;
import com.example.eshop.ai.enums.*;
import com.example.eshop.ai.registry.*;
import com.example.eshop.ai.service.*;
import com.example.eshop.ai.repository.*;
import com.example.eshop.common.audit.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.slf4j.MDC;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;
import java.util.*;
import java.time.Instant;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:ai;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=create-drop",
    "eureka.client.enabled=false","spring.cloud.discovery.enabled=false","spring.kafka.admin.auto-create=false",
    "jwt.secret=66546A5744444446E5A7234743777217A25432A462D4A614E645267556B587032733576"})
@AutoConfigureMockMvc
class AiOrchestrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AiExecutionRepository executions;
    @Autowired NotificationOutboxRepository outbox;
    @Autowired AuditLogRepository audits;
    @MockitoBean AiProviderClient provider;
    @MockitoBean CatalogToolClient catalog;
    @MockitoBean OrderToolClient orders;
    @MockitoBean NotificationOutboxPublisher publisher;
    static String token(long id,String... roles) {
        return "Bearer "+Jwts.builder().setSubject("user-"+id).claim("userId",id).claim("isEnable",true)
            .claim("authorities",List.of(roles)).setExpiration(Date.from(Instant.now().plusSeconds(300)))
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode("66546A5744444446E5A7234743777217A25432A462D4A614E645267556B587032733576"))).compact();
    }
    void intent(AiIntent intent,String params) throws Exception {
        when(provider.detect(anyString(),anyList())).thenReturn(new AiIntentResult(intent,.98,mapper.readTree(params)));
    }
    org.springframework.test.web.servlet.ResultActions request(String bearer,String id) throws Exception {
        return mvc.perform(post("/api/ai/execute").header("Authorization",bearer).header("X-Request-ID","req_ai_test")
            .header("Idempotency-Key",id).contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"my safe test request\"}"));
    }
    @Test void knownIntentUsesFixedClientAndPreservesIdentity() throws Exception {
        intent(AiIntent.SKU_GET,"{\"skuId\":100}");
        when(catalog.sku(anyString(),eq(100L))).thenReturn(mapper.readTree("{\"id\":100}"));
        String key=UUID.randomUUID().toString(), bearer=token(7,"USER");
        request(bearer,key).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.requestId").value("req_ai_test")).andExpect(header().string("X-Request-ID","req_ai_test"));
        verify(catalog).sku(bearer,100);
        assertThat(executions.findById(UUID.fromString(key)).orElseThrow().getUserId()).isEqualTo(7);
        assertThat(MDC.get("requestId")).isNull();
    }
    @Test void unknownIntentDoesNotExecute() throws Exception {
        intent(AiIntent.UNKNOWN,"{}");
        request(token(7),UUID.randomUUID().toString()).andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(catalog,orders);
    }
    @Test void missingAndInvalidParametersDoNotExecute() throws Exception {
        intent(AiIntent.SKU_GET,"{}");
        request(token(7),UUID.randomUUID().toString()).andExpect(jsonPath("$.errorCode").value("MISSING_PARAMETER"));
        intent(AiIntent.SKU_GET,"{\"skuId\":-1}");
        request(token(7),UUID.randomUUID().toString()).andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETERS"));
        verifyNoInteractions(catalog);
    }
    @Test void injectionCannotChooseUrlOrSensitiveTool() throws Exception {
        intent(AiIntent.SKU_GET,"{\"skuId\":1,\"url\":\"http://payment-service/refund\"}");
        request(token(7),UUID.randomUUID().toString()).andExpect(status().isUnprocessableEntity());
        intent(AiIntent.PAYMENT_REFUND,"{}");
        request(token(7,"ADMIN"),UUID.randomUUID().toString()).andExpect(status().isForbidden()).andExpect(jsonPath("$.errorCode").value("TOOL_DISABLED"));
        verifyNoInteractions(catalog,orders);
    }
    @Test void unauthorizedToolsAreHiddenAndRejected() throws Exception {
        when(provider.detect(anyString(),anyList())).thenAnswer(call -> {
            List<AiToolDefinition> supplied=call.getArgument(1);
            assertThat(supplied).noneMatch(t->t.requiredPermission().equals("ADMIN") || !t.enabled());
            return new AiIntentResult(AiIntent.INVENTORY_LOW_STOCK,.98,mapper.readTree("{}"));
        });
        request(token(7,"USER"),UUID.randomUUID().toString()).andExpect(status().isForbidden());
        verifyNoInteractions(catalog);
    }
    @Test void percentageOver100IsRejected() throws Exception {
        intent(AiIntent.PROMOTION_CREATE,"{\"skuId\":501,\"name\":\"Sale\",\"discount\":200,\"startAt\":\"2026-11-01T00:00:00\",\"endAt\":\"2026-11-02T00:00:00\"}");
        request(token(7,"ADMIN"),UUID.randomUUID().toString()).andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(catalog);
    }
    @Test void providerTimeoutIsAuditedWithoutExecution() throws Exception {
        when(provider.detect(anyString(),anyList())).thenThrow(new AiFailure("AI_PROVIDER_TIMEOUT",AiExecutionStatus.FAILURE,"AI unavailable."));
        String key=UUID.randomUUID().toString();
        request(token(7),key).andExpect(status().isBadGateway()).andExpect(jsonPath("$.errorCode").value("AI_PROVIDER_TIMEOUT"));
        assertThat(executions.findById(UUID.fromString(key)).orElseThrow().getStatus()).isEqualTo(AiExecutionStatus.FAILURE);
        verifyNoInteractions(catalog,orders);
    }
    @Test void successfulWriteIsAuditedEnqueuedAndNotRepeated() throws Exception {
        intent(AiIntent.PROMOTION_CREATE,"{\"skuId\":501,\"name\":\"Sale\",\"discount\":20,\"startAt\":\"2026-11-01T00:00:00\",\"endAt\":\"2026-11-02T00:00:00\"}");
        when(catalog.createPromotion(anyString(),any())).thenReturn(mapper.readTree("{\"id\":77}"));
        String key=UUID.randomUUID().toString();
        request(token(7,"ADMIN"),key).andExpect(status().isOk());
        request(token(7,"ADMIN"),key).andExpect(status().isOk());
        verify(catalog,times(1)).createPromotion(anyString(),any());
        assertThat(outbox.findAll()).anyMatch(e->e.getPayload().contains(key) && e.getPayload().contains("req_ai_test"));
        var records=audits.findAll((root,q,cb)->cb.equal(root.get("resourceId"),"77"));
        assertThat(records).anyMatch(a->a.getAction()==AuditAction.AI_TOOL_EXECUTION && a.getResult()==AuditResult.SUCCESS && "7".equals(a.getActorId()));
        assertThat(records).allMatch(a->!a.getNewValue().contains("my safe test request"));
        request(token(8,"ADMIN"),key).andExpect(status().isForbidden());
    }
    @Test void downstreamFailureIsRecorded() throws Exception {
        intent(AiIntent.SKU_GET,"{\"skuId\":1}");
        when(catalog.sku(anyString(),anyLong())).thenThrow(new RuntimeException("secret must not escape"));
        request(token(7),UUID.randomUUID().toString()).andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.message").value("The operation could not be completed safely."));
    }
    @Test void frontendIdentityCannotBeInjected() throws Exception {
        mvc.perform(post("/api/ai/execute").header("Authorization",token(7)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\":\"show SKU 1\",\"userId\":99}")).andExpect(status().isBadRequest());
        verifyNoInteractions(provider);
    }
    @Test void unauthenticatedRequestIsRejected() throws Exception {
        mvc.perform(post("/api/ai/execute").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"show SKU 1\"}")).andExpect(status().isUnauthorized());
    }
}
