package com.example.eshop.ai;
import com.example.eshop.ai.service.AiChatStateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.core.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.Duration;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class AiGuestConversationIsolationTest {
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    @Test void suppliedGuestConversationIdCannotExposeAnotherGuestsDialogue(){
        var redis=mock(StringRedisTemplate.class);var service=new AiChatStateService(redis,new ObjectMapper());UUID id=UUID.randomUUID();
        SecurityContextHolder.clearContext();
        service.rememberDialogue(id,"private guest question","private answer");
        assertThat(service.dialogue(id)).isEmpty();
        verifyNoInteractions(redis);
    }
    @Test @SuppressWarnings("unchecked") void authenticatedDialogueKeepsActorNamespaceAndExpiration(){
        var redis=mock(StringRedisTemplate.class);ValueOperations<String,String> values=mock(ValueOperations.class);when(redis.opsForValue()).thenReturn(values);
        var auth=new UsernamePasswordAuthenticationToken("user",null,List.of());auth.setDetails(Map.of("userId",7L));SecurityContextHolder.getContext().setAuthentication(auth);
        var service=new AiChatStateService(redis,new ObjectMapper());UUID id=UUID.randomUUID();service.rememberDialogue(id,"question","answer");
        verify(values).set(eq("ai:dialogue:user:7:"+id),anyString(),eq(Duration.ofMinutes(30)));
    }
}
