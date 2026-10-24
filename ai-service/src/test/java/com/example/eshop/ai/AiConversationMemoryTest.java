package com.example.eshop.ai;

import com.example.eshop.ai.service.AiConversationMemory;
import com.example.eshop.common.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiConversationMemoryTest {
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 private void actor(long id){var a=new UsernamePasswordAuthenticationToken("test","",List.of());a.setDetails(Map.of("userId",id));SecurityContextHolder.getContext().setAuthentication(a);}
 @Test void guestsNeverPersistAndNonAdminsCannotManageMemory(){
  var jdbc=mock(JdbcTemplate.class);var permissions=mock(LivePermissionService.class);
  when(permissions.current()).thenReturn(new PermissionSnapshot(17,false,Set.of()));
  var memory=new AiConversationMemory(jdbc,permissions);
  memory.save(UUID.randomUUID(),false,"test","user","CHAT","hello");verifyNoInteractions(jdbc);
  actor(17);assertThatThrownBy(()->memory.list(0)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
  assertThatThrownBy(()->memory.relevant("past",true)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
  verifyNoInteractions(jdbc);
 }
 @Test @EnabledIfEnvironmentVariable(named="AI_TRAINING_TEST_URL",matches=".+")
 void ownershipScopeRelevanceIdempotencyAndAdminDeletion(){
  var data=new DriverManagerDataSource(System.getenv("AI_TRAINING_TEST_URL"),System.getenv("AI_TRAINING_TEST_USERNAME"),System.getenv("AI_TRAINING_TEST_PASSWORD"));
  var jdbc=new JdbcTemplate(data);var tx=new TransactionTemplate(new DataSourceTransactionManager(data));
  var permissions=mock(LivePermissionService.class);when(permissions.current()).thenReturn(new PermissionSnapshot(17,true,Set.of()));
  var memory=new AiConversationMemory(jdbc,permissions);
  tx.execute(status->{status.setRollbackOnly();actor(17);UUID id=UUID.randomUUID();String marker="memorytest"+id;
   memory.save(id,false,"call:1","user","CALL",marker);
   memory.save(id,false,"call:1","user","CALL",marker); // Network retry is idempotent.
   memory.save(id,true,"chat:1","user","CHAT","admin private note");
   assertThat(memory.recent(id,false)).hasSize(1);assertThat(memory.relevant(marker,false)).hasSize(1);
   assertThat(memory.recent(id,true)).extracting(r->r.get("content")).containsExactly("admin private note");
   actor(18);assertThat(memory.owns(id)).isFalse();assertThat(memory.recent(id,false)).isEmpty();assertThat(memory.relevant(marker,false)).isEmpty();
   actor(17);assertThat(memory.owns(id)).isTrue();assertThat(memory.delete(id,17,"CUSTOMER")).isEqualTo(1);
   assertThat(memory.recent(id,false)).isEmpty();assertThat(memory.relevant(marker,false)).isEmpty();
   assertThat(memory.recent(id,true)).hasSize(1);return null;
  });
 }
}
