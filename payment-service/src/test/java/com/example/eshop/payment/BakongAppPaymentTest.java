package com.example.eshop.payment;

import com.example.eshop.common.audit.AuditLogService;
import com.example.eshop.payment.automation.*;
import com.example.eshop.payment.bakong.service.BakongTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class BakongAppPaymentTest {
    final PaymentAutomation payments=mock(PaymentAutomation.class);
    final BakongTokenService tokens=mock(BakongTokenService.class);
    final AuditLogService audit=mock(AuditLogService.class);
    final RestClient.Builder builder=RestClient.builder();
    final MockRestServiceServer server=MockRestServiceServer.bindTo(builder).build();
    final BakongAppPayment service=new BakongAppPayment(payments,builder.build(),tokens,audit,new org.springframework.transaction.support.TransactionTemplate(new TestTransactions()));
    static class TestTransactions extends org.springframework.transaction.support.AbstractPlatformTransactionManager {
        protected Object doGetTransaction(){return new Object();}
        protected void doBegin(Object transaction,org.springframework.transaction.TransactionDefinition definition){}
        protected void doCommit(org.springframework.transaction.support.DefaultTransactionStatus status){}
        protected void doRollback(org.springframework.transaction.support.DefaultTransactionStatus status){}
    }
    BakongAppPaymentTest(){
        doAnswer(call->{
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())
                .as("AuditLogService requires an active transaction after provider handoff").isTrue();
            return null;
        }).when(audit).record(any());
        when(payments.customerAppQr(9)).thenReturn(payment("PENDING",Instant.now().plusSeconds(60)));
        when(tokens.getToken()).thenReturn("test-token");
        ReflectionTestUtils.setField(service,"enabled",true);
        ReflectionTestUtils.setField(service,"provider","https://provider.example");
        ReflectionTestUtils.setField(service,"returnOrigin","https://shop.example");
        ReflectionTestUtils.setField(service,"allowedHosts","approved.example");
        ReflectionTestUtils.setField(service,"icon","https://shop.example/icon.png");
        ReflectionTestUtils.setField(service,"name","E-Shop");
    }
    Map<String,Object> payment(String status,Instant expiry){return Map.of("paymentId",4L,"orderId",9L,"status",status,"orderStatus","PENDING","expiresAt",expiry,"qr","server-owned-qr","amount",10,"currency","USD");}
    @Test void generatesProviderLinkFromOwnedQrWithFixedReturnAndNeverVerifies(){
        server.expect(requestTo("https://provider.example/v1/generate_deeplink_by_qr"))
            .andExpect(method(HttpMethod.POST)).andExpect(header("Authorization","Bearer test-token"))
            .andExpect(content().json("{\"qr\":\"server-owned-qr\",\"sourceInfo\":{\"appName\":\"E-Shop\",\"appIconUrl\":\"https://shop.example/icon.png\",\"appDeepLinkCallback\":\"https://shop.example/checkout/payment/9?bakongReturn=1\"}}"))
            .andRespond(withSuccess("{\"responseCode\":0,\"data\":{\"shortLink\":\"https://approved.example/pay/abc\"}}",MediaType.APPLICATION_JSON));
        assertThat(service.launch(9)).containsEntry("deepLink","https://approved.example/pay/abc").doesNotContainKey("qr");
        verify(payments).customerAppQr(9);verifyNoMoreInteractions(payments);server.verify();
    }
    @Test void rejectsUnapprovedProviderLink(){
        server.expect(anything()).andRespond(withSuccess("{\"responseCode\":0,\"data\":{\"shortLink\":\"https://evil.example/pay\"}}",MediaType.APPLICATION_JSON));
        assertThatThrownBy(()->service.launch(9)).hasMessageContaining("503");verifyNoInteractions(audit);
    }
    @Test void rejectsExpiredAndCompletedPaymentsWithoutProviderCall(){
        when(payments.customerAppQr(9)).thenReturn(payment("PENDING",Instant.now().minusSeconds(1)));
        assertThatThrownBy(()->service.launch(9)).hasMessageContaining("409");
        when(payments.customerAppQr(9)).thenReturn(payment("SUCCESS",Instant.now().plusSeconds(60)));
        assertThatThrownBy(()->service.launch(9)).hasMessageContaining("409");verifyNoInteractions(tokens);server.verify();
    }
    @Test void ownershipFailureNeverCallsProvider(){
        when(payments.customerAppQr(9)).thenThrow(new org.springframework.security.access.AccessDeniedException("Unavailable"));
        assertThatThrownBy(()->service.launch(9)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);verifyNoInteractions(tokens);
    }
    @Test void rejectsUnsafeReturnUrlAndMissingConfiguration(){
        ReflectionTestUtils.setField(service,"returnOrigin","http://shop.example");
        assertThatThrownBy(()->service.launch(9)).hasMessageContaining("503");verifyNoInteractions(tokens);
    }
    @Test void disabledHandoffPreservesQrFallback(){
        ReflectionTestUtils.setField(service,"enabled",false);
        assertThatThrownBy(()->service.launch(9)).hasMessageContaining("503");verifyNoInteractions(tokens);
    }

    @Test void ipModeUsesProviderGeneratedLinkWithoutUnsupportedCallbackMetadata(){
        ReflectionTestUtils.setField(service,"sourceInfoEnabled",false);
        ReflectionTestUtils.setField(service,"icon","");
        server.expect(requestTo("https://provider.example/v1/generate_deeplink_by_qr"))
            .andExpect(header("Authorization","Bearer test-token"))
            .andExpect(content().json("{\"qr\":\"server-owned-qr\"}",true))
            .andRespond(withSuccess("{\"responseCode\":0,\"data\":{\"shortLink\":\"https://approved.example/payment\"}}",MediaType.APPLICATION_JSON));
        assertThat(service.launch(9)).containsEntry("automaticReturn",false).containsEntry("deepLink","https://approved.example/payment");
        verify(payments).customerAppQr(9);verifyNoMoreInteractions(payments);server.verify();
    }
}
