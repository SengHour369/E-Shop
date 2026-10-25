package com.example.eshop.ai.voice;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.assertj.core.api.Assertions.*;

class GeminiAudioTranscriptionTest {
    @Test void sendsValidatedAudioToOnlyTheApprovedFreeModelAndReturnsKhmerDraft(){
        var builder=RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta");
        var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent"))
            .andExpect(header("x-goog-api-key","server-only-key"))
            .andExpect(jsonPath("$.contents[0].parts[1].inlineData.mimeType").value("audio/webm"))
            .andExpect(jsonPath("$.contents[0].parts[1].inlineData.data").value("AQID"))
            .andExpect(jsonPath("$.tools").doesNotExist())
            .andRespond(withSuccess("{\"candidates\":[{\"content\":{\"parts\":[{\"thought\":true,\"text\":\"private reasoning\"},{\"text\":\"សួស្តី AI\"}]}}]}",MediaType.APPLICATION_JSON));
        var service=new GeminiAudioTranscriptionService("server-only-key",true,"gemini-3.1-flash-lite",true,builder.build());
        assertThat(service.transcribe(new byte[]{1,2,3},"audio/webm","km")).isEqualTo("សួស្តី AI");server.verify();
    }
    @Test void billingAttestationAndApprovedModelAreRequiredAndThereIsNoFallback(){
        assertThat(new GeminiAudioTranscriptionService("key",true,"paid-model",true,RestClient.create()).available()).isFalse();
        assertThat(new GeminiAudioTranscriptionService("key",true,"gemini-3.1-flash-lite",false,RestClient.create()).available()).isFalse();
        assertThat(new GeminiAudioTranscriptionService("",true,"gemini-3.1-flash-lite",true,RestClient.create()).available()).isFalse();
    }
    @Test void freeQuotaExhaustionIsReportedWithoutRetryingOrDisclosingProviderDetails(){
        var builder=RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta");var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("private provider response"));
        var service=new GeminiAudioTranscriptionService("secret",true,"gemini-3.1-flash-lite",true,builder.build());
        assertThatThrownBy(()->service.transcribe(new byte[]{1},"audio/wav","en")).hasMessageContaining("429").hasMessageNotContaining("private");server.verify();
    }
    @Test void silenceIsNeverReportedAsSuccessfulTranscription(){
        var builder=RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta");var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(anything()).andRespond(withSuccess("{\"candidates\":[]}",MediaType.APPLICATION_JSON));
        var service=new GeminiAudioTranscriptionService("key",true,"gemini-3.1-flash-lite",true,builder.build());
        assertThatThrownBy(()->service.transcribe(new byte[]{0},"audio/wav","en")).hasMessageContaining("422");server.verify();
    }
    @Test void providerReadTimeoutReturnsAnActionableTimeoutWithoutLeakingDetails(){
        var builder=RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta");
        var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(anything()).andRespond(request->{throw new java.net.SocketTimeoutException("private details");});
        var service=new GeminiAudioTranscriptionService("secret",true,"gemini-3.1-flash-lite",true,builder.build());
        assertThatThrownBy(()->service.transcribe(new byte[]{1},"audio/wav","en")).hasMessageContaining("504").hasMessageContaining("shorter recording").hasMessageNotContaining("private details");server.verify();
    }
}
