package com.example.eshop.ai.controller;

import com.example.eshop.ai.service.*;
import com.example.eshop.ai.voice.GeminiAudioTranscriptionService;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiVoiceGeminiRoutingTest {
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    @Test void configuredGeminiCannotBeUsedByAnonymousUploads(){
        var gemini=mock(GeminiAudioTranscriptionService.class);when(gemini.available()).thenReturn(true);
        var controller=new AiVoiceController(mock(AiChatStateService.class),mock(AiVoiceAudit.class),"","","ffprobe",gemini);
        assertThatThrownBy(()->controller.transcribe(new MockMultipartFile("audio","recording","audio/webm",new byte[]{1}),"en",new MockHttpServletRequest()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(gemini,never()).transcribe(any(),any(),any());
    }
    @Test void authenticatedBrowserWebmGoesThroughValidationThenGeminiAndReturnsEditableKhmerText() throws Exception{
        var actor=new UsernamePasswordAuthenticationToken("test",null,List.of());actor.setDetails(Map.of("userId",42L));SecurityContextHolder.getContext().setAuthentication(actor);
        var gemini=mock(GeminiAudioTranscriptionService.class);when(gemini.available()).thenReturn(true);
        when(gemini.transcribe(new byte[]{1,2},"audio/webm","km")).thenReturn("សួស្តី");
        Path probe=Files.createTempFile("voice-gemini-probe-",".sh");
        try{
            Files.writeString(probe,"#!/bin/sh\nprintf '%s' '{\"streams\":[{\"codec_type\":\"audio\"}],\"format\":{\"duration\":\"2\"}}'\n");probe.toFile().setExecutable(true);
            var audit=mock(AiVoiceAudit.class);
            var controller=new AiVoiceController(mock(AiChatStateService.class),audit,"","",probe.toString(),gemini);
            var result=controller.transcribe(new MockMultipartFile("audio","recording","audio/webm;codecs=opus",new byte[]{1,2}),"km",new MockHttpServletRequest());
            assertThat(result.getBody().text()).isEqualTo("សួស្តី");assertThat(result.getHeaders().getCacheControl()).isEqualTo("no-store");
            verify(gemini).transcribe(new byte[]{1,2},"audio/webm","km");verify(audit).accepted("km",2);
        }finally{Files.deleteIfExists(probe);}
    }
}
