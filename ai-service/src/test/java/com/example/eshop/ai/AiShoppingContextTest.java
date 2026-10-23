package com.example.eshop.ai;
import com.example.eshop.ai.service.AiChatStateService;
import com.example.eshop.ai.dto.AiResponse;
import com.example.eshop.ai.enums.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.core.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import java.time.Duration;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;
class AiShoppingContextTest {
 final ObjectMapper mapper=new ObjectMapper();
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void remembersRealCheckoutAndOrderReferencesWithoutInventingIds() throws Exception {
  var auth=new UsernamePasswordAuthenticationToken("customer","unused",List.of());auth.setDetails(Map.of("userId",7));SecurityContextHolder.getContext().setAuthentication(auth);
  var redis=mock(StringRedisTemplate.class);ValueOperations<String,String> ops=mock(ValueOperations.class);when(redis.opsForValue()).thenReturn(ops);
  when(redis.execute(any(org.springframework.data.redis.core.script.RedisScript.class),anyList(),any(),any())).thenReturn(1L);
  var state=new AiChatStateService(redis,mapper);var conversation=UUID.randomUUID();
  state.remember(conversation,new AiResponse(null,null,null,AiIntent.CHECKOUT_PREVIEW,AiExecutionStatus.SUCCESS,"review",null,mapper.readTree("{\"reviewId\":\"review-123\",\"addressId\":12,\"paymentMethod\":\"BAKONG\"}")));
  var values=org.mockito.ArgumentCaptor.forClass(String.class);verify(redis).execute(any(org.springframework.data.redis.core.script.RedisScript.class),anyList(),values.capture(),any());assertThat(mapper.readTree(values.getValue()).path("reviewId").asText()).isEqualTo("review-123");
  reset(redis);when(redis.execute(any(org.springframework.data.redis.core.script.RedisScript.class),anyList(),any(),any())).thenReturn(1L);when(ops.get(anyString())).thenReturn(values.getValue());
  state.remember(conversation,new AiResponse(null,null,null,AiIntent.ORDER_CREATE,AiExecutionStatus.SUCCESS,"created",null,mapper.readTree("{\"id\":55,\"order_number\":\"ORD-55\"}")));
  verify(redis).execute(any(org.springframework.data.redis.core.script.RedisScript.class),anyList(),values.capture(),eq("[\"reviewId\"]"));var result=mapper.readTree(values.getValue());assertThat(result.path("selectedOrderId").asLong()).isEqualTo(55);assertThat(result.has("reviewId")).isFalse();
 }
 @Test void deniedOperationsDoNotUpdateConversationReferences(){
  var redis=mock(StringRedisTemplate.class);new AiChatStateService(redis,mapper).remember(UUID.randomUUID(),new AiResponse(null,null,null,AiIntent.ORDER_CREATE,AiExecutionStatus.DENIED,"denied",null,mapper.createObjectNode().put("id",99)));verifyNoInteractions(redis);
 }
}
