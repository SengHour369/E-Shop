package com.example.eshop.ai.service;
import com.example.eshop.ai.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
@DataJpaTest(properties={"spring.datasource.url=jdbc:h2:mem:quota;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop","eureka.client.enabled=false"})
@Transactional(propagation=Propagation.NOT_SUPPORTED)
class AiQuotaServiceTest {
 @Autowired AiDailyQuotaRepository quotas;@Autowired AiQuotaRequestRepository requests;@Autowired PlatformTransactionManager transactions;
 AiQuotaService service;final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
 void actor(long id,String role){var auth=new UsernamePasswordAuthenticationToken("user",null,List.of(new SimpleGrantedAuthority(role)));auth.setDetails(Map.of("userId",id));SecurityContextHolder.getContext().setAuthentication(auth);}
 AiQuotaService fresh(){var s=new AiQuotaService(quotas,requests,mapper,mock(StringRedisTemplate.class),transactions,5,"Asia/Phnom_Penh");s.clock(Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"),ZoneOffset.UTC));return s;}
 @BeforeEach void setup(){requests.deleteAll();quotas.deleteAll();service=fresh();actor(11,"USER");}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 ResponseEntity<Map> accept(UUID key,String message){return service.process(key,message,Map.class,()->ResponseEntity.ok(Map.of("answer","ok")));}
 @Test void sixthIsRejectedAndUsersAreIndependent(){for(int i=0;i<5;i++)accept(UUID.randomUUID(),"prompt"+i);assertThat(service.current().used()).isEqualTo(5);assertThatThrownBy(()->accept(UUID.randomUUID(),"sixth")).isInstanceOf(AiQuotaService.Exhausted.class);actor(12,"USER");assertThat(service.current().used()).isZero();accept(UUID.randomUUID(),"other");assertThat(service.current().used()).isEqualTo(1);}
 @Test void administratorsAreUnlimited(){for(String role:List.of("ADMIN","SUPER_ADMIN")){actor(role.equals("ADMIN")?20:21,role);for(int i=0;i<7;i++)accept(UUID.randomUUID(),"prompt"+i);assertThat(service.current().unlimited()).isTrue();assertThat(service.current().used()).isZero();}}
 @Test void idempotentRetryReplaysWithoutCallingProvider(){UUID key=UUID.randomUUID();AtomicInteger calls=new AtomicInteger();for(int i=0;i<2;i++)service.process(key,"same",Map.class,()->{calls.incrementAndGet();return ResponseEntity.ok(Map.of("answer","once"));});assertThat(calls).hasValue(1);assertThat(service.current().used()).isEqualTo(1);assertThatThrownBy(()->accept(key,"changed")).hasMessageContaining("another request");assertThat(service.current().used()).isEqualTo(1);}
 @Test void durableUsageResetsExactlyAtCambodiaMidnight(){accept(UUID.randomUUID(),"first");assertThat(service.current().resetAt().toString()).isEqualTo("2026-10-16T00:00+07:00");service=fresh();service.clock(Clock.fixed(Instant.parse("2026-10-15T16:59:59Z"),ZoneOffset.UTC));assertThat(service.current().used()).isEqualTo(1);service.clock(Clock.fixed(Instant.parse("2026-10-15T17:00:00Z"),ZoneOffset.UTC));assertThat(service.current().used()).isZero();accept(UUID.randomUUID(),"next day");assertThat(service.current().used()).isEqualTo(1);}
 @Test void providerFailureIsChargedOnce(){UUID key=UUID.randomUUID();assertThatThrownBy(()->service.process(key,"failure",Map.class,()->{throw new IllegalStateException("Provider failed");})).hasMessageContaining("Provider failed");assertThat(service.current().used()).isEqualTo(1);assertThatThrownBy(()->accept(key,"failure")).hasMessageContaining("accepted request failed");assertThat(service.current().used()).isEqualTo(1);}
 @Test void concurrentRequestsCannotExceedFive()throws Exception{var pool=Executors.newFixedThreadPool(8);var gate=new CountDownLatch(1);List<Future<Boolean>> tasks=new ArrayList<>();try{for(int i=0;i<8;i++){final int n=i;tasks.add(pool.submit(()->{actor(11,"USER");gate.await();try{accept(UUID.randomUUID(),"concurrent"+n);return true;}catch(AiQuotaService.Exhausted e){return false;}finally{SecurityContextHolder.clearContext();}}));}gate.countDown();int count=0;for(var task:tasks)if(task.get(30,TimeUnit.SECONDS))count++;assertThat(count).isEqualTo(5);assertThat(service.current().used()).isEqualTo(5);}finally{pool.shutdownNow();}}
 @Test void structuredButtonsRemainIdempotentWithoutConsumingModelQuota(){
   var key=UUID.randomUUID();var calls=new AtomicInteger();
   for(int i=0;i<2;i++)service.processRegistered(key,"cart",Map.class,()->{calls.incrementAndGet();return ResponseEntity.ok(Map.of("ok",true));});
   assertThat(calls).hasValue(1);assertThat(service.current().used()).isZero();
 }
 @Test void firstInsertCannotOverwriteExistingUsage(){
   accept(UUID.randomUUID(),"first");
   var staleNewRow=new com.example.eshop.ai.model.AiDailyQuota();staleNewRow.setUserId(11L);staleNewRow.setBusinessDay(LocalDate.of(2026,10,15));
   assertThatThrownBy(()->quotas.saveAndFlush(staleNewRow)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
   assertThat(service.current().used()).isEqualTo(1);
 }
 @Test void unauthenticatedIdentityCannotConsumeQuota(){SecurityContextHolder.clearContext();assertThatThrownBy(()->accept(UUID.randomUUID(),"invalid")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);assertThat(requests.count()).isZero();}
 @Test void feedbackBelongsToTheAuthenticatedResponseOwner(){UUID key=UUID.randomUUID();accept(key,"answer");service.feedback(key,"up");assertThat(requests.findById("11:"+key).orElseThrow().getFeedback()).isEqualTo("up");actor(12,"USER");assertThatThrownBy(()->service.feedback(key,"down")).hasMessageContaining("not found");assertThat(requests.findById("11:"+key).orElseThrow().getFeedback()).isEqualTo("up");}
}

