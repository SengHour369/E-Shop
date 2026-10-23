package com.example.eshop.ai.voice;
import com.example.eshop.ai.service.AiChatStateService;
import com.example.eshop.ai.dto.AiResponse;
import com.example.eshop.ai.enums.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="VOICE_REDIS_TEST",matches="true")
class VoiceContextRedisTest {
 final long actorId=9_000_000_000L+Math.abs(UUID.randomUUID().getLeastSignificantBits()%100_000_000);
 final ObjectMapper mapper=new ObjectMapper(); final UUID conversation=UUID.randomUUID();
 LettuceConnectionFactory factory;StringRedisTemplate redis;AiChatStateService state;
 void actor(){var auth=new UsernamePasswordAuthenticationToken("isolated-test","",List.of());auth.setDetails(Map.of("userId",actorId));SecurityContextHolder.getContext().setAuthentication(auth);}
 void remember(AiIntent intent,String json)throws Exception{state.remember(conversation,new AiResponse(null,null,null,intent,AiExecutionStatus.SUCCESS,"actual",null,mapper.readTree(json)));}
 @BeforeEach void setup(){factory=new LettuceConnectionFactory("127.0.0.1",6379);factory.afterPropertiesSet();redis=new StringRedisTemplate(factory);state=new AiChatStateService(redis,mapper);actor();state.conversation(null);}
 @AfterEach void cleanup(){redis.delete("ai:conversation:"+actorId+":"+conversation);var keys=redis.keys("ai:conversation:"+actorId+":*");if(keys!=null&&!keys.isEmpty())redis.delete(keys);factory.destroy();SecurityContextHolder.clearContext();}
 @Test void retainsOrderSelectionSkuAndVoiceWithoutLostConcurrentUpdates()throws Exception{
  remember(AiIntent.PRODUCT_SEARCH,"[{\"id\":11},{\"id\":22}]");remember(AiIntent.PRODUCT_DETAIL,"{\"id\":22}");
  var pool=Executors.newFixedThreadPool(2);
  try{var tasks=List.<Callable<Void>>of(()->{actor();remember(AiIntent.PRODUCT_STOCK_CHECK,"{\"id\":42}");return null;},()->{actor();state.voice(conversation,UUID.randomUUID(),"km",Instant.now().plusSeconds(60),true);return null;});for(var task:pool.invokeAll(tasks))task.get();}finally{pool.shutdownNow();}
  state.displayedProducts(conversation,List.of(22L,11L));var refs=state.references(conversation);
  assertThat(refs.path("selectedProductId").asLong()).isEqualTo(22);assertThat(refs.path("selectedSkuId").asLong()).isEqualTo(42);assertThat(refs.path("voiceCallActive").asBoolean()).isTrue();assertThat(refs.path("productIdsInDisplayOrder").get(0).asLong()).isEqualTo(22);
  remember(AiIntent.PRODUCT_DETAIL,"{\"id\":11}");assertThat(state.references(conversation).has("selectedSkuId")).isFalse();
  assertThatThrownBy(()->state.displayedProducts(conversation,List.of(99L))).hasMessageContaining("verified search");
 }
 @Test void emptySearchRetainsArrayTypeAndExpiredVoiceIsInactive()throws Exception{
  remember(AiIntent.PRODUCT_SEARCH,"[]");state.voice(conversation,UUID.randomUUID(),"en",Instant.now().minusSeconds(1),true);
  assertThat(state.references(conversation).path("productIdsInDisplayOrder").isArray()).isTrue();assertThat(state.references(conversation).path("voiceCallActive").asBoolean()).isFalse();
 }
}
