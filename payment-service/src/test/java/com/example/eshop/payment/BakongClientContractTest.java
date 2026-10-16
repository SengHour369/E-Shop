package com.example.eshop.payment;

import com.example.eshop.payment.automation.BakongVerificationPolicy;
import com.example.eshop.payment.bakong.dto.CheckTransactionRequest;
import com.example.eshop.payment.bakong.service.BakongTokenService;
import com.example.eshop.payment.bakong.service.impl.BakongServiceImpl;
import com.example.eshop.payment.bakong.service.impl.BakongTokenServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/** Wire-contract regression for the owner's working Order_Shop Bakong implementation. */
class BakongClientContractTest {
    static final String BASE="https://api-bakong.nbc.gov.kh";
    final ObjectMapper mapper=new ObjectMapper();

    @Test void renewsTokenWithRegisteredEmailAndCachesUntilExpiry() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        var service=new BakongTokenServiceImpl(builder.build(),mapper);
        ReflectionTestUtils.setField(service,"baseUrl",BASE+"/");
        ReflectionTestUtils.setField(service,"email","registered@example.com");
        String payload="{\"exp\":"+Instant.now().plusSeconds(900).getEpochSecond()+"}";
        String token="test."+Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))+".signature";
        server.expect(requestTo(BASE+"/v1/renew_token")).andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("{\"email\":\"registered@example.com\"}"))
            .andRespond(withSuccess("{\"responseCode\":0,\"data\":{\"token\":\""+token+"\"}}",MediaType.APPLICATION_JSON));
        assertThat(service.getToken()).isEqualTo(token);
        assertThat(service.getToken()).isEqualTo(token);
        server.verify();
    }

    @Test void md5LookupUsesBearerTokenAndPreservesEvidenceForAuthoritativeValidation() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        var tokens=mock(BakongTokenService.class);when(tokens.getToken()).thenReturn("test-provider-token");
        var service=new BakongServiceImpl(builder.build(),mapper,tokens);
        ReflectionTestUtils.setField(service,"baseUrl",BASE+"/");
        String md5="a".repeat(32),hash="b".repeat(64);
        server.expect(requestTo(BASE+"/v1/check_transaction_by_md5")).andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization","Bearer test-provider-token"))
            .andExpect(content().json("{\"md5\":\""+md5+"\"}"))
            .andRespond(withSuccess("{\"responseCode\":0,\"data\":{\"hash\":\""+hash+"\",\"amount\":10,\"currency\":\"USD\",\"toAccountId\":\"registered@bank\"}}",MediaType.APPLICATION_JSON));
        var response=service.checkTransactionByMD5(new CheckTransactionRequest(md5));
        assertThat(BakongVerificationPolicy.verifiedHash(response,BigDecimal.TEN,"USD","registered@bank",mapper)).isEqualTo(hash);
        assertThatThrownBy(()->BakongVerificationPolicy.verifiedHash(response,BigDecimal.ONE,"USD","registered@bank",mapper)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->BakongVerificationPolicy.verifiedHash(response,BigDecimal.TEN,"USD","another@bank",mapper)).isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @Test void providerNotFoundRemainsUnpaid() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        var tokens=mock(BakongTokenService.class);when(tokens.getToken()).thenReturn("test-token");
        var service=new BakongServiceImpl(builder.build(),mapper,tokens);ReflectionTestUtils.setField(service,"baseUrl",BASE);
        server.expect(requestTo(BASE+"/v1/check_transaction_by_md5"))
            .andRespond(withSuccess("{\"responseCode\":1,\"errorCode\":1,\"responseMessage\":\"Transaction could not be found\"}",MediaType.APPLICATION_JSON));
        var response=service.checkTransactionByMD5(new CheckTransactionRequest("0".repeat(32)));
        assertThat(BakongVerificationPolicy.notFound(response)).isTrue();
        assertThat(BakongVerificationPolicy.verifiedHash(response,BigDecimal.TEN,"USD","registered@bank",mapper)).isNull();
        server.verify();
    }

    @Test void unregisteredEmailCannotSupplyVerificationToken() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        var service=new BakongTokenServiceImpl(builder.build(),mapper);
        ReflectionTestUtils.setField(service,"baseUrl",BASE);ReflectionTestUtils.setField(service,"email","unregistered@example.com");
        server.expect(requestTo(BASE+"/v1/renew_token"))
            .andRespond(withSuccess("{\"responseCode\":1,\"errorCode\":10,\"responseMessage\":\"Not registered yet\"}",MediaType.APPLICATION_JSON));
        assertThatThrownBy(service::getToken).isInstanceOf(RuntimeException.class);
        server.verify();
    }
}
