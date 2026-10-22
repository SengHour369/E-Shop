package com.example.eshop.ai.controller;

import com.example.eshop.ai.service.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.web.server.ResponseStatusException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiVoiceControllerTest {
    final AiChatStateService state=mock(AiChatStateService.class);
    final AiVoiceAudit audit=mock(AiVoiceAudit.class);
    final MockHttpServletRequest request=new MockHttpServletRequest();
    AiVoiceController controller(String url,String probe) {return new AiVoiceController(state,audit,url,"secret",probe);}
    @Test void unavailableProviderAndInvalidUploadsFailSafely() {
        var file=new MockMultipartFile("audio","recording.webm","audio/webm",new byte[]{1});
        assertStatus(503,()->controller("","ffprobe").transcribe(file,"km",request));
        assertStatus(400,()->controller("http://localhost","ffprobe").transcribe(file,"fr",request));
        assertStatus(415,()->controller("http://localhost","ffprobe").transcribe(new MockMultipartFile("audio","x","text/plain",new byte[]{1}),"en",request));
        assertStatus(413,()->controller("http://localhost","ffprobe").transcribe(new MockMultipartFile("audio",new byte[0]),"en",request));
        verifyNoInteractions(audit);
    }
    @Test void rateLimitRunsBeforeAnyProviderWork() {
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS)).when(state).rateLimit(any());
        assertStatus(429,()->controller("http://localhost","ffprobe").transcribe(new MockMultipartFile("audio",new byte[]{1}),"en",request));
        verifyNoInteractions(audit);
    }
    @Test void validatesDurationAndReturnsOnlyEditableText() throws Exception {
        Path probe=Files.createTempFile("voice-test-probe-",".sh");
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/transcribe",exchange->{
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer secret");
            String body=new String(exchange.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
            assertThat(body).contains("name=\"language\"","km");
            byte[] result="{\"text\":\"សួស្តី\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,result.length);
            exchange.getResponseBody().write(result);exchange.close();
        });server.start();
        try {
            probe.toFile().setExecutable(true);
            Files.writeString(probe,"#!/bin/sh\nprintf '%s' '{\"streams\":[{\"codec_type\":\"audio\"}],\"packets\":[{\"pts_time\":\"2\",\"duration_time\":\"0.02\"}]}'\n");
            var controller=controller("http://127.0.0.1:"+server.getAddress().getPort()+"/transcribe",probe.toString());
            var file=new MockMultipartFile("audio","x.webm","audio/webm",new byte[]{1,2});
            var response=controller.transcribe(file,"km",request);
            assertThat(response.getBody().text()).isEqualTo("សួស្តី");
            assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
            verify(audit).accepted("km",2);
            Files.writeString(probe,"#!/bin/sh\nprintf '%s' '{\"streams\":[{\"codec_type\":\"audio\"}],\"format\":{\"duration\":\"62\"}}'\n");
            assertStatus(400,()->controller.transcribe(file,"km",request));
        } finally {server.stop(0);Files.deleteIfExists(probe);}
    }
    void assertStatus(int expected,org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(expected));
    }
}
