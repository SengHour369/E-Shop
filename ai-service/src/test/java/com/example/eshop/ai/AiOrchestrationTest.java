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
    @Autowired com.example.eshop.ai.repository.AiDailyQuotaRepository dailyQuotas;
    @Autowired com.example.eshop.ai.repository.AiQuotaRequestRepository quotaRequests;
    @Autowired NotificationOutboxRepository outbox;
    @Autowired AuditLogRepository audits;
    @MockitoBean AiProviderClient provider;
    @MockitoBean AiConversationService conversation;
    @MockitoBean CatalogToolClient catalog;
    @MockitoBean OrderToolClient orders;
    @MockitoBean NotificationOutboxPublisher publisher;
    @MockitoBean com.example.eshop.common.security.LivePermissionService permissions;
    @MockitoBean AiChatStateService chatState;
    @MockitoBean AiConfirmationService confirmations;
    @MockitoBean PaymentToolClient payments;
    @MockitoBean NotificationToolClient notifications;
    @MockitoBean AuthToolClient auth;

    @Test
    void accountListRequiresLiveFunctionAvailabilityAndUsesFixedClient() throws Exception {
        intent(AiIntent.ADMIN_USER_LIST, "{}");
        request(token(7, "ADMIN"), UUID.randomUUID().toString())
                .andExpect(status().isForbidden());
        verifyNoInteractions(auth);
        doReturn(new com.example.eshop.common.security.PermissionSnapshot(
                7L, true, Set.of("USER_VIEW"))).when(permissions).current();
        when(auth.users(anyString())).thenReturn(mapper.readTree("[]"));
        String bearer = token(7, "ADMIN");
        request(bearer, UUID.randomUUID().toString())
                .andExpect(status().isOk());
        verify(auth).users(bearer);
    }

    @Test
    void revenueToolRequiresAnExplicitDate() throws Exception {
        doReturn(new com.example.eshop.common.security.PermissionSnapshot(
                7L, true, Set.of("REPORT_VIEW"))).when(permissions).current();
        intent(AiIntent.ADMIN_REVENUE_SUMMARY, "{}");
        request(token(7, "ADMIN"), UUID.randomUUID().toString())
                .andExpect(jsonPath("$.errorCode").value("MISSING_PARAMETER"));
        verifyNoInteractions(payments);
    }

    @BeforeEach
    void currentPermissions() {
        quotaRequests.deleteAll(); dailyQuotas.deleteAll();
        when(permissions.current()).thenAnswer(call -> {
            boolean admin = com.example.eshop.common.security.CurrentActor.has("ADMIN");
            return new com.example.eshop.common.security.PermissionSnapshot(
                    com.example.eshop.common.security.CurrentActor.userId(), admin,
                    admin ? Set.of("PROMOTION_MANAGE", "INVENTORY_VIEW", "ORDER_VIEW_ALL") : Set.of());
        });
    }
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
        UUID confirmationId = UUID.randomUUID();
        var parameters = mapper.readTree("{\"skuId\":501,\"name\":\"Sale\",\"discount\":20,\"startAt\":\"2026-11-01T00:00:00\",\"endAt\":\"2026-11-02T00:00:00\"}");
        when(confirmations.prepare(eq(AiIntent.PROMOTION_CREATE), any())).thenReturn(
                mapper.createObjectNode().put("confirmationId", confirmationId.toString()));
        when(confirmations.consume(confirmationId)).thenReturn(
                new AiConfirmationService.Pending(AiIntent.PROMOTION_CREATE, parameters));
        request(token(7,"ADMIN"),key).andExpect(status().isUnprocessableEntity());
        verify(catalog, never()).createPromotion(anyString(), any());
        mvc.perform(post("/api/ai/confirm/" + confirmationId).header("Authorization", token(7,"ADMIN"))
                .header("X-Request-ID", "req_ai_test")).andExpect(status().isOk());
        mvc.perform(post("/api/ai/confirm/" + confirmationId).header("Authorization", token(7,"ADMIN")))
                .andExpect(status().isOk());
        verify(catalog,times(1)).createPromotion(anyString(),any());
        assertThat(outbox.findAll()).anyMatch(e->e.getPayload().contains(confirmationId.toString()) && e.getPayload().contains("req_ai_test"));
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

    @Test
    void guestCanSearchPublicProducts() throws Exception {
        intent(AiIntent.PRODUCT_SEARCH, "{\"query\":\"shoes\"}");
        when(catalog.search(isNull(), eq("shoes"), isNull(), isNull(), isNull())).thenReturn(mapper.readTree("[{\"id\":42}]"));
        mvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Show shoes\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.data[0].id").value(42));
        verifyNoInteractions(permissions);
    }

    @Test
    void productSearchForwardsValidatedPriceAndStockFilters() throws Exception {
        intent(AiIntent.PRODUCT_SEARCH, "{\"query\":\"phone\",\"maxPrice\":300,\"inStock\":true}");
        when(catalog.search(isNull(), eq("phone"), isNull(), eq(new java.math.BigDecimal("300")), eq(true)))
                .thenReturn(mapper.readTree("[{\"id\":24,\"name\":\"Phone\",\"images\":[\"https://example.test/phone.png\"],\"skus\":[{\"id\":88,\"effectivePrice\":240,\"available\":5}]}]"));
        mvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"រកទូរស័ព្ទតម្លៃក្រោម ៣០០ ដុល្លារ\",\"language\":\"km\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cardType").value("ProductList"))
                .andExpect(jsonPath("$.result.data[0].skus[0].effectivePrice").value(240));
        verify(catalog).search(isNull(), eq("phone"), isNull(), eq(new java.math.BigDecimal("300")), eq(true));
    }

    @Test
    void invalidProductPriceRangeCannotCallCatalog() throws Exception {
        intent(AiIntent.PRODUCT_SEARCH, "{\"query\":\"phone\",\"minPrice\":500,\"maxPrice\":300}");
        request(token(7), UUID.randomUUID().toString()).andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETERS"));
        verifyNoInteractions(catalog);
    }

    @Test
    void emptyProductSearchHasExplicitNoResultsMessage() throws Exception {
        intent(AiIntent.PRODUCT_SEARCH, "{\"query\":\"phone\"}");
        when(catalog.search(isNull(), eq("phone"), isNull(), isNull(), isNull())).thenReturn(mapper.readTree("[]"));
        mvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Find phones\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.message").value(org.hamcrest.Matchers.containsString("No matching products")));
    }

    @Test
    void productServiceFailureIsSafeAndNotPresentedAsProducts() throws Exception {
        intent(AiIntent.PRODUCT_SEARCH, "{\"query\":\"phone\"}");
        when(catalog.search(isNull(), eq("phone"), isNull(), isNull(), isNull())).thenThrow(new IllegalStateException("private upstream details"));
        mvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Find phones\"}"))
                .andExpect(jsonPath("$.cardType").value("ErrorCard"))
                .andExpect(jsonPath("$.result.message").value("Product search is temporarily unavailable. Please try again."));
    }

    @Test
    void guestCannotReadPrivateOrders() throws Exception {
        intent(AiIntent.MY_ORDERS, "{}");
        mvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Show my orders\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(orders);
    }

    @Test
    void administratorCannotUseUnavailableFunction() throws Exception {
        doReturn(new com.example.eshop.common.security.PermissionSnapshot(7, true, Set.of()))
                .when(permissions).current();
        intent(AiIntent.ADMIN_ORDER_LIST, "{}");
        request(token(7, "SUPER_ADMIN"), UUID.randomUUID().toString())
                .andExpect(status().isForbidden());
        verifyNoInteractions(orders);
    }

    @Test
    void grantedAdminReadUsesFixedClient() throws Exception {
        intent(AiIntent.ADMIN_ORDER_LIST, "{\"status\":\"FAILED\"}");
        when(orders.admin(anyString(), eq("FAILED"), isNull())).thenReturn(mapper.readTree("[]"));
        String bearer = token(7, "ADMIN");
        request(bearer, UUID.randomUUID().toString()).andExpect(status().isOk());
        verify(orders).admin(bearer, "FAILED", null);
    }

    @Test
    void disabledCurrentAccountCannotUseStaleJwt() throws Exception {
        doThrow(new org.springframework.security.access.AccessDeniedException("disabled")).when(permissions).current();
        request(token(7, "ADMIN"), UUID.randomUUID().toString()).andExpect(status().isForbidden());
        verifyNoInteractions(provider, catalog, orders);
    }

    @Test
    void invalidBearerCannotDowngradeToGuest() throws Exception {
        mvc.perform(post("/api/ai/chat").header("Authorization", "Bearer invalid")
                .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Show shoes\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(provider);
    }

    @Test
    void fakeModeIsRejectedBeforeInference() throws Exception {
        mvc.perform(post("/api/ai/chat").header("Authorization", token(7))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Show all payments\",\"mode\":\"SUPER_ADMIN\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(provider);
    }

    @Test
    void lowConfidenceWriteCannotPrepareConfirmation() throws Exception {
        when(provider.detect(anyString(), anyList())).thenReturn(
                new AiIntentResult(AiIntent.PROMOTION_CREATE, 0.2, mapper.readTree("{}")));
        request(token(7, "ADMIN"), UUID.randomUUID().toString()).andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(confirmations, catalog);
    }

    @Test
    void knowledgeWithoutApprovedDocumentsDoesNotInventPolicy() throws Exception {
        when(conversation.reply(anyString(), any(), anyList(), eq(false))).thenReturn("I don't have the verified E-Shop return policy. Please contact support.");
        intent(AiIntent.KNOWLEDGE_SEARCH, "{\"query\":\"return policy\"}");
        mvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"What is the return policy?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardType").value("ConversationCard"))
                .andExpect(jsonPath("$.result.message").value("I don't have the verified E-Shop return policy. Please contact support."));
    }

    @Test
    void cancellationRequiresPreviewAndExplicitConfirmation() throws Exception {
        intent(AiIntent.ORDER_CANCEL, "{\"orderNumber\":\"ORD-42\"}");
        UUID confirmationId = UUID.randomUUID();
        var parameters = mapper.readTree("{\"orderNumber\":\"ORD-42\"}");
        when(orders.cancelPreview(anyString(), eq("ORD-42")))
                .thenReturn(mapper.readTree("{\"eligible\":true,\"order\":{\"id\":42,\"status\":\"PENDING\"}}"));
        when(confirmations.prepare(eq(AiIntent.ORDER_CANCEL), any()))
                .thenReturn(mapper.createObjectNode().put("confirmationId", confirmationId.toString()));
        request(token(7), UUID.randomUUID().toString())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.data.preview.order.id").value(42));
        verify(orders, never()).cancel(anyString(), anyString());
        when(confirmations.consume(confirmationId))
                .thenReturn(new AiConfirmationService.Pending(AiIntent.ORDER_CANCEL, parameters));
        when(orders.cancel(anyString(), eq("ORD-42")))
                .thenReturn(mapper.readTree("{\"id\":42,\"status\":\"CANCELLED\"}"));
        mvc.perform(post("/api/ai/confirm/" + confirmationId).header("Authorization", token(7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        verify(orders, times(1)).cancel(anyString(), eq("ORD-42"));
    }

    @Test
    void revokedPermissionStopsPreviouslyPreparedWrite() throws Exception {
        UUID id = UUID.randomUUID();
        when(confirmations.consume(id)).thenReturn(new AiConfirmationService.Pending(
                AiIntent.PROMOTION_DISABLE, mapper.readTree("{\"id\":42}")));
        doReturn(new com.example.eshop.common.security.PermissionSnapshot(7, true, Set.of()))
                .when(permissions).current();
        mvc.perform(post("/api/ai/confirm/" + id).header("Authorization", token(7, "ADMIN")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(catalog);
    }

    @Test
    void expiredAccessTokenIsRejected() throws Exception {
        String expired = "Bearer " + Jwts.builder().setSubject("user-7")
                .claim("userId", 7).claim("isEnable", true)
                .setExpiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(
                        "66546A5744444446E5A7234743777217A25432A462D4A614E645267556B587032733576")))
                .compact();
        request(expired, UUID.randomUUID().toString()).andExpect(status().isUnauthorized());
        verifyNoInteractions(provider);
    }
    @Test
    void greetingWorksWithoutTheModelOrBusinessTools() throws Exception {
        UUID id = UUID.randomUUID();
        when(chatState.conversation(null)).thenReturn(id);
        mvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Hello\",\"language\":\"en\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardType").value("ConversationCard"))
                .andExpect(jsonPath("$.result.message").value("Hello! How can I help? You can ask about products, orders or payments."));
        verifyNoInteractions(conversation, provider, catalog, orders);
        verify(chatState).rememberDialogue(id, "Hello", "Hello! How can I help? You can ask about products, orders or payments.",false);
    }

    @Test
    void uncertainChatGetsNaturalReplyWithoutExecutingTools() throws Exception {
        UUID id = UUID.randomUUID();
        when(chatState.conversation(null)).thenReturn(id);
        when(chatState.dialogue(id)).thenReturn(List.of());
        when(provider.detect(anyString(), anyList())).thenReturn(
                new AiIntentResult(AiIntent.UNKNOWN, 0.1, mapper.readTree("{}")));
        when(conversation.reply("What is Bluetooth?", "en", List.of(), false)).thenReturn("Bluetooth connects devices wirelessly.");
        mvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"What is Bluetooth?\",\"language\":\"en\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.message").value("Bluetooth connects devices wirelessly."));
        verifyNoInteractions(catalog, orders, confirmations);
    }

    @Test
    void mistakenToolWithoutRequiredParametersFallsBackWithoutExecutingIt() throws Exception {
        UUID id = UUID.randomUUID();
        when(chatState.conversation(null)).thenReturn(id);
        when(chatState.dialogue(id)).thenReturn(List.of());
        when(provider.detect(anyString(), anyList())).thenReturn(
                new AiIntentResult(AiIntent.SKU_GET, 0.95, mapper.readTree("{}")));
        when(conversation.reply("Explain Bluetooth", "en", List.of(), false)).thenReturn("Bluetooth connects devices wirelessly.");
        mvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Explain Bluetooth\",\"language\":\"en\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardType").value("ConversationCard"));
        verifyNoInteractions(catalog, orders, confirmations);
    }

    @Test
    void conversationOutageRemainsARetryableError() throws Exception {
        UUID id = UUID.randomUUID();
        when(chatState.conversation(null)).thenReturn(id);
        mvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"How are you\",\"language\":\"en\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.result.errorCode").value("AI_PROVIDER_FAILURE"));
        verifyNoInteractions(provider, catalog, orders);
    }    @Test void authenticatedCustomerChatHasFiveDailyRequestsAndInvalidBodiesAreFree() throws Exception {
        String bearer=token(70,"USER");
        mvc.perform(post("/api/ai/chat").header("Authorization",bearer).contentType("application/json").content("{\"message\":\"\",\"language\":\"en\"}"))
                .andExpect(status().isBadRequest());
        assertThat(quotaRequests.count()).isZero();
        String key=UUID.randomUUID().toString();
        for(int i=0;i<5;i++) mvc.perform(post("/api/ai/chat").header("Authorization",bearer).header("Idempotency-Key",i==0?key:UUID.randomUUID().toString()).contentType("application/json").content("{\"message\":\"Hi\",\"language\":\"en\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/ai/chat").header("Authorization",bearer).header("Idempotency-Key",key).contentType("application/json").content("{\"message\":\"Hi\",\"language\":\"en\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/ai/chat").header("Authorization",bearer).header("Idempotency-Key",UUID.randomUUID().toString()).contentType("application/json").content("{\"message\":\"Hi\",\"language\":\"en\"}"))
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("AI_DAILY_LIMIT_EXCEEDED")).andExpect(jsonPath("$.used").value(5));
        mvc.perform(get("/api/ai/quota").header("Authorization",bearer)).andExpect(status().isOk()).andExpect(jsonPath("$.remaining").value(0));
    }
    @Test void revokedAccountCannotReplayCachedAiResponse() throws Exception {
        String bearer = token(901, "USER"); String key = UUID.randomUUID().toString();
        intent(AiIntent.SKU_GET, "{\"skuId\":1}");
        when(catalog.sku(anyString(), anyLong())).thenReturn(mapper.readTree("{\"id\":1}"));
        request(bearer, key).andExpect(status().isOk());
        doThrow(new org.springframework.security.access.AccessDeniedException("disabled")).when(permissions).current();
        request(bearer, key).andExpect(status().isForbidden());
        verify(catalog, times(1)).sku(anyString(), anyLong());
        verify(provider, times(1)).detect(anyString(), anyList());
    }

    @Test void revokedFunctionCannotReplayPreviouslyCachedAdminData() throws Exception {
        String bearer = token(902, "ADMIN"); String key = UUID.randomUUID().toString();
        intent(AiIntent.ADMIN_ORDER_LIST, "{}");
        when(orders.admin(anyString(), isNull(), isNull())).thenReturn(mapper.readTree("[]"));
        request(bearer, key).andExpect(status().isOk());
        doReturn(new com.example.eshop.common.security.PermissionSnapshot(902, true, Set.of())).when(permissions).current();
        request(bearer, key).andExpect(status().isForbidden());
        verify(orders, times(1)).admin(anyString(), isNull(), isNull());
    }

    @Test void executeKeyCannotMasqueradeAsAConfirmationKey() throws Exception {
        String bearer = token(903, "USER"); UUID key = UUID.randomUUID();
        intent(AiIntent.SKU_GET, "{\"skuId\":1}");
        when(catalog.sku(anyString(), anyLong())).thenReturn(mapper.readTree("{\"id\":1}"));
        request(bearer, key.toString()).andExpect(status().isOk());
        mvc.perform(post("/api/ai/confirm/" + key).header("Authorization", bearer)).andExpect(status().isConflict());
        verify(confirmations, never()).consume(any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
        "{\"success\":false,\"status\":200}",
        "{\"errorCode\":\"PROVIDER_ERROR\"}",
        "{\"errorCode\":\"1\"}",
        "{\"status\":\"503\"}",
        "null"
    })
    void errorEnvelopesCannotBecomeSuccessfulToolResults(String body) throws Exception {
        intent(AiIntent.SKU_GET, "{\"skuId\":1}");
        when(catalog.sku(anyString(), anyLong())).thenReturn(mapper.readTree(body));
        request(token(904, "ADMIN"), UUID.randomUUID().toString()).andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.errorCode").value("DOWNSTREAM_REJECTED"));
    }

    @Test void customerIdentityCannotBePassedAsAToolArgument() throws Exception {
        intent(AiIntent.MY_ORDERS, "{\"userId\":999}");
        request(token(905, "USER"), UUID.randomUUID().toString()).andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(orders);
    }

    @Test void structuredProductDetailsCannotChooseAnArbitraryEndpoint() throws Exception {
        when(catalog.product(anyString(), eq(24L))).thenReturn(mapper.readTree("{\"id\":24,\"name\":\"Phone\",\"skus\":[]}"));
        mvc.perform(post("/api/ai/tools/PRODUCT_DETAIL").header("Authorization",token(7)).header("Idempotency-Key",UUID.randomUUID().toString())
                .contentType("application/json").content("{\"id\":24}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(24));
        mvc.perform(post("/api/ai/tools/PRODUCT_DETAIL").header("Authorization",token(7)).header("Idempotency-Key",UUID.randomUUID().toString())
                .contentType("application/json").content("{\"id\":24,\"url\":\"https://unapproved.example\"}"))
                .andExpect(status().isUnprocessableEntity());
        verify(catalog,times(1)).product(anyString(),eq(24L)); verifyNoInteractions(provider);
    }

    @Test void cartWritesRequireConfirmationAndForwardTheConfirmationKey() throws Exception {
        UUID confirmation = UUID.randomUUID();
        var parameters = mapper.readTree("{\"skuId\":88,\"quantity\":2}");
        when(confirmations.prepare(eq(AiIntent.CART_ADD), any())).thenReturn(mapper.readTree("{\"confirmationId\":\""+confirmation+"\"}"));
        mvc.perform(post("/api/ai/tools/CART_ADD").header("Authorization",token(7)).header("Idempotency-Key",UUID.randomUUID().toString())
                .contentType("application/json").content(parameters.toString()))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.errorCode").value("CONFIRMATION_REQUIRED"));
        verifyNoInteractions(orders);
        when(confirmations.consume(confirmation)).thenReturn(new AiConfirmationService.Pending(AiIntent.CART_ADD,parameters));
        when(orders.changeCart(anyString(),eq(confirmation.toString()),eq("add"),eq(parameters))).thenReturn(mapper.readTree("{\"items\":[],\"total_price\":20}"));
        mvc.perform(post("/api/ai/confirm/"+confirmation).header("Authorization",token(7)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCESS"));
        mvc.perform(post("/api/ai/confirm/"+confirmation).header("Authorization",token(7))).andExpect(status().isOk());
        verify(orders,times(1)).changeCart(anyString(),eq(confirmation.toString()),eq("add"),eq(parameters));
    }

    @Test void adminDraftRejectsCustomersAndRevokedDomainPermissionBeforeStaging() throws Exception {
        var parameters=mapper.createObjectNode().put("toolType","CATEGORY_BULK_CREATE")
            .put("valuesJson","{\"name\":\"Phone\",\"status\":true}");
        mvc.perform(post("/api/ai/tools/ADMIN_IMPORT_DRAFT").header("Authorization",token(7)).header("Idempotency-Key",UUID.randomUUID().toString())
                .contentType("application/json").content(parameters.toString())).andExpect(status().isForbidden());
        doReturn(new com.example.eshop.common.security.PermissionSnapshot(7,true,Set.of("IMPORT_UPLOAD"))).when(permissions).current();
        doThrow(new org.springframework.security.access.AccessDeniedException("Denied")).when(permissions).require("CATEGORY_CREATE");
        mvc.perform(post("/api/ai/tools/ADMIN_IMPORT_DRAFT").header("Authorization",token(7,"ADMIN")).header("Idempotency-Key",UUID.randomUUID().toString())
                .contentType("application/json").content(parameters.toString())).andExpect(status().isForbidden());
        verifyNoInteractions(confirmations,catalog);
    }

    @Test void structuredToolsCannotSpoofCustomerOrCallDisabledRefunds() throws Exception {
        mvc.perform(post("/api/ai/tools/CART_GET").header("Authorization",token(7)).header("Idempotency-Key",UUID.randomUUID().toString())
                .contentType("application/json").content("{\"userId\":8}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/ai/tools/RETURN_CREATE").header("Authorization",token(7)).header("Idempotency-Key",UUID.randomUUID().toString())
                .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(orders);
    }

    @Test void photoToolIsAuthenticatedIdempotentAndBoundToTheUploadRequest() throws Exception {
        var image = new org.springframework.mock.web.MockMultipartFile("image","photo.jpg","image/jpeg",new byte[]{(byte)255,(byte)216,(byte)255,0});
        when(catalog.recognize(anyString(),any())).thenReturn(mapper.readTree("{\"code\":200,\"data\":{\"matchType\":\"AI_VISUAL_COMPARISON\",\"requiresSelection\":true,\"candidates\":[{\"productId\":24,\"name\":\"Phone\",\"confidence\":0.8}]}}"));
        String key = UUID.randomUUID().toString();
        for(int i=0;i<2;i++) mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/ai/visual-search")
                .file(image).header("Authorization",token(7)).header("Idempotency-Key",key))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.candidates[0].productId").value(24));
        verify(catalog,times(1)).recognize(anyString(),any()); verifyNoInteractions(provider);
        mvc.perform(post("/api/ai/tools/PRODUCT_VISUAL_SEARCH").header("Authorization",token(7)).header("Idempotency-Key",UUID.randomUUID().toString())
                .contentType("application/json").content("{\"uploadId\":\"invented\"}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.errorCode").value("IMAGE_ATTACHMENT_REQUIRED"));
        verify(catalog,times(1)).recognize(anyString(),any());
    }
    @Test void invalidPhotoIsRejectedWithoutCallingVision() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/ai/visual-search")
                .file(new org.springframework.mock.web.MockMultipartFile("image","file.txt","text/plain",new byte[]{1}))
                .header("Authorization",token(7)).header("Idempotency-Key",UUID.randomUUID().toString()))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.errorCode").value("INVALID_IMAGE"));
        verifyNoInteractions(catalog,provider);
    }

}




