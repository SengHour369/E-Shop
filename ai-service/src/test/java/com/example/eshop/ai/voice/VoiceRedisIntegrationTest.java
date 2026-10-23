package com.example.eshop.ai.voice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="VOICE_REDIS_TEST",matches="true")
class VoiceRedisIntegrationTest {
    private LettuceConnectionFactory factory;private StringRedisTemplate redis;private VoiceSessions sessions;
    private final long actorId=8_000_000_000L+Math.abs(UUID.randomUUID().getLeastSignificantBits()%100_000_000);
    private String root(){return "ai:voice:{"+actorId+"}:";}
    private void actor(){var auth=new UsernamePasswordAuthenticationToken("test","",List.of());auth.setDetails(Map.of("userId",actorId));SecurityContextHolder.getContext().setAuthentication(auth);}
    @BeforeEach void setup(){factory=new LettuceConnectionFactory("127.0.0.1",6379);factory.afterPropertiesSet();redis=new StringRedisTemplate(factory);sessions=new VoiceSessions(redis,new ObjectMapper().registerModule(new JavaTimeModule()),new VoiceProperties(true,"gemini-3.1-flash-live-preview",true,false,true,5,300,1));actor();}
    @AfterEach void cleanup(){var keys=redis.keys(root()+"*");if(keys!=null&&!keys.isEmpty())redis.delete(keys);factory.destroy();SecurityContextHolder.clearContext();}
    @Test void atomicReservationsAcrossDevicesAllowOnlyOneActiveCall() throws Exception{
        var pool=Executors.newFixedThreadPool(8);
        try{
            var tasks=new ArrayList<Callable<Boolean>>();for(int i=0;i<16;i++)tasks.add(()->{actor();try{sessions.reserve(UUID.randomUUID(),"km");return true;}catch(org.springframework.web.server.ResponseStatusException e){assertThat(e.getStatusCode().value()).isEqualTo(409);return false;}finally{SecurityContextHolder.clearContext();}});
            long accepted=0;for(var result:pool.invokeAll(tasks))if(result.get())accepted++;
            assertThat(accepted).isEqualTo(1);assertThat(sessions.retryAfterSeconds()).isBetween(1L,300L);assertThat(sessions.usage().get("usedToday")).isEqualTo(0);assertThat(sessions.usage().get("reservedToday")).isEqualTo(1);
        }finally{pool.shutdownNow();}
    }
    @Test void failedProvisioningDoesNotCountConnectedAckIsIdempotentAndDailyLimitSurvivesEnd(){
        var failed=sessions.reserve(UUID.randomUUID(),"km");sessions.provisioningFailed(failed);
        assertThat(sessions.usage().get("remainingToday")).isEqualTo(5);
        for(int i=0;i<5;i++){
            var s=sessions.reserve(UUID.randomUUID(),"km");sessions.connected(s);sessions.connected(s);sessions.end(s);
            assertThat(sessions.usage().get("usedToday")).isEqualTo(i+1);
            assertThatThrownBy(()->sessions.reserve(UUID.randomUUID(),"km")).hasMessageContaining("409");
            // Advance the lease in the test's own namespace without waiting five minutes.
            redis.delete(root()+"active");
        }
        assertThatThrownBy(()->sessions.reserve(UUID.randomUUID(),"km")).hasMessageContaining("429");
        assertThat(sessions.usage().get("remainingToday")).isEqualTo(0);
    }
    @Test void otherUsersCannotReadSession(){
        var s=sessions.reserve(UUID.randomUUID(),"en");
        var auth=new UsernamePasswordAuthenticationToken("other","",List.of());auth.setDetails(Map.of("userId",actorId+1));SecurityContextHolder.getContext().setAuthentication(auth);
        assertThatThrownBy(()->sessions.owned(s.sessionId())).hasMessageContaining("410");
    }
}
