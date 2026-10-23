package com.example.eshop.ai.voice;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.*;
import java.time.Instant;
import java.util.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.assertj.core.api.Assertions.*;
class GeminiVoiceTokenTest {
    @Test void verifiesModelSupportsBidirectionalStreamingBeforeProvisioning(){
        var builder=RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1alpha");
        var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1alpha/models/gemini-3.1-flash-live-preview"))
            .andExpect(method(HttpMethod.GET)).andRespond(withSuccess("{\"supportedGenerationMethods\":[\"bidiGenerateContent\"]}",MediaType.APPLICATION_JSON));
        new GeminiVoiceTokenService("key",builder.build()).verifyModel("gemini-3.1-flash-live-preview");server.verify();
    }
    @Test void provisionsSingleUseModelRestrictedCredentialWithoutPuttingKeyInUrl(){
        var builder=RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1alpha");
        var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1alpha/auth_tokens"))
            .andExpect(method(HttpMethod.POST)).andExpect(header("x-goog-api-key","server-only-secret"))
            .andExpect(jsonPath("$.uses").value(1))
            .andExpect(jsonPath("$.expireTime").value("2026-10-23T12:05:00Z"))
            .andExpect(jsonPath("$.bidiGenerateContentSetup.model").value("models/gemini-3.1-flash-live-preview"))
            .andExpect(jsonPath("$.bidiGenerateContentSetup.generationConfig.responseModalities[0]").value("AUDIO"))
            .andExpect(jsonPath("$.bidiGenerateContentSetup.inputAudioTranscription").isMap())
            .andRespond(withSuccess("{\"name\":\"auth_tokens/test\"}",MediaType.APPLICATION_JSON));
        var service=new GeminiVoiceTokenService("server-only-secret",builder.build());
        assertThat(service.issue("gemini-3.1-flash-live-preview",Map.of("systemInstruction",Map.of("parts",List.of(Map.of("text","Reply in Khmer"))),"tools",List.of()),Instant.parse("2026-10-23T12:05:00Z"))).isEqualTo("auth_tokens/test");
        server.verify();
    }
    @Test void providerQuotaAndErrorsAreRedactedAndNeverRetried(){
        var builder=RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1alpha");var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("secret provider details"));
        var service=new GeminiVoiceTokenService("secret",builder.build());
        assertThatThrownBy(()->service.issue("x",Map.of("systemInstruction",Map.of(),"tools",List.of()),Instant.now().plusSeconds(300)))
            .hasMessageContaining("Free AI voice calling is temporarily unavailable").hasMessageNotContaining("secret");
        server.verify();
    }
}
